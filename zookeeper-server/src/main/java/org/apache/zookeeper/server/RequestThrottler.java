/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.zookeeper.server;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.zookeeper.common.Time;
import org.apache.zookeeper.util.ServiceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 当启用时，该类会限制外部提交到请求处理流水线。增强了在连接层{@link NIOServerCnxn}
 * {@link NettyServerCnxn} 的功能。
 * 连接层在TCP 连接请求超出限制时取消选举。但是连接层允许一个连接在取消选举前至少请求一次。
 * 因此，在某种情况下40000 个客户端连接，请求数可能为40000
 * 该类通过增加额外的队列解决了该问题。当启用时，客户端连接的请求不会提交至请求处理流水线，
 * 而是提交到该类。如果总请求数大于{@link #maxRequests}，会阻塞住{@link #stallTime}
 * 直到低于限制
 *
 * When enabled, the RequestThrottler limits the number of outstanding requests
 * currently submitted to the request processor pipeline. The throttler augments
 * the limit imposed by the <code>globalOutstandingLimit</code> that is enforced
 * by the connection layer ({@link NIOServerCnxn}, {@link NettyServerCnxn}).
 *
 * The connection layer limit applies backpressure against the TCP connection by
 * disabling selection on connections once the request limit is reached. However,
 * the connection layer always allows a connection to send at least one request
 * before disabling selection on that connection. Thus, in a scenario with 40000
 * client connections, the total number of requests inflight may be as high as
 * 40000 even if the <code>globalOustandingLimit</code> was set lower.
 *
 * The RequestThrottler addresses this issue by adding additional queueing. When
 * enabled, client connections no longer submit requests directly to the request
 * processor pipeline but instead to the RequestThrottler. The RequestThrottler
 * is then responsible for issuing requests to the request processors, and
 * enforces a separate <code>maxRequests</code> limit. If the total number of
 * outstanding requests is higher than <code>maxRequests</code>, the throttler
 * will continually stall for <code>stallTime</code> milliseconds until
 * underlimit.
 *
 * The RequestThrottler can also optionally drop stale requests rather than
 * submit them to the processor pipeline. A stale request is a request sent
 * by a connection that is already closed, and/or a request whose latency
 * will end up being higher than its associated session timeout. The notion
 * of staleness is configurable, @see Request for more details.
 *
 * To ensure ordering guarantees, if a request is ever dropped from a connection
 * that connection is closed and flagged as invalid. All subsequent requests
 * inflight from that connection are then dropped as well.
 */
public class RequestThrottler extends ZooKeeperCriticalThread {

    private static final Logger LOG = LoggerFactory.getLogger(RequestThrottler.class);

    private final LinkedBlockingQueue<Request> submittedRequests = new LinkedBlockingQueue<Request>();

    private final ZooKeeperServer zks;
    private volatile boolean stopping;
    private volatile boolean killed;

    private static final String SHUTDOWN_TIMEOUT = "zookeeper.request_throttler.shutdownTimeout";
    private static int shutdownTimeout = 10000;

    static {
        shutdownTimeout = Integer.getInteger(SHUTDOWN_TIMEOUT, 10000);
        LOG.info("{} = {}", SHUTDOWN_TIMEOUT, shutdownTimeout);
    }

    /**
     * 在阻塞请求前允许的最大请求数
     * The total number of outstanding requests allowed before the throttler
     * starts stalling.
     *
     * When maxRequests = 0, throttling is disabled.
     */
    private static volatile int maxRequests = Integer.getInteger("zookeeper.request_throttle_max_requests", 0);

    /**
     * 在允许处理请求前的暂停时间
     * The time (in milliseconds) this is the maximum time for which throttler
     * thread may wait to be notified that it may proceed processing a request.
     */
    private static volatile int stallTime = Integer.getInteger("zookeeper.request_throttle_stall_time", 100);

    /**
     * 当为true 时，限流器会放弃过期请求，而不是放到请求流水线中。过期请求为过期会话的请求
     * When true, the throttler will drop stale requests rather than issue
     * them to the request pipeline. A stale request is a request sent by
     * a connection that is now closed, and/or a request that will have a
     * request latency higher than the sessionTimeout. The staleness of
     * a request is tunable property, @see Request for details.
     */
    private static volatile boolean dropStaleRequests = Boolean.parseBoolean(System.getProperty("zookeeper.request_throttle_drop_stale", "true"));

    /**
     * 1.请求可以限流
     * 2.有配置限流等待时间{@link ZooKeeperServer#throttledOpWaitTime}
     * 3.给定参数elapsedTime > {@link ZooKeeperServer#throttledOpWaitTime}
     * @param request
     * @param elapsedTime
     * @return
     */
    protected boolean shouldThrottleOp(Request request, long elapsedTime) {
        return request.isThrottlable()
                && zks.getThrottledOpWaitTime() > 0
                && elapsedTime > zks.getThrottledOpWaitTime();
    }


    public RequestThrottler(ZooKeeperServer zks) {
        super("RequestThrottler", zks.getZooKeeperServerListener());
        this.zks = zks;
        this.stopping = false;
        this.killed = false;
    }

    public static int getMaxRequests() {
        return maxRequests;
    }

    public static void setMaxRequests(int requests) {
        maxRequests = requests;
    }

    public static int getStallTime() {
        return stallTime;
    }

    public static void setStallTime(int time) {
        stallTime = time;
    }

    public static boolean getDropStaleRequests() {
        return dropStaleRequests;
    }

    public static void setDropStaleRequests(boolean drop) {
        dropStaleRequests = drop;
    }

    @Override
    public void run() {
        try {
            while (true) {
                if (killed) {
                    break;
                }

                Request request = submittedRequests.take();
                if (Request.requestOfDeath == request) {
                    break;
                }

                if (request.mustDrop()) {
                    continue;
                }

                // Throttling is disabled when maxRequests = 0
                if (maxRequests > 0) {
                    while (!killed) {
                        if (dropStaleRequests && request.isStale()) {
                            // Note: this will close the connection
                            dropRequest(request);
                            ServerMetrics.getMetrics().STALE_REQUESTS_DROPPED.add(1);
                            request = null;
                            break;
                        }
                        if (zks.getInProcess() < maxRequests) {
                            break;
                        }
                        // 超出运行最大请求数，阻塞等待
                        throttleSleep(stallTime);
                    }
                }

                if (killed) {
                    break;
                }

                // A dropped stale request will be null
                if (request != null) {
                    if (request.isStale()) {
                        ServerMetrics.getMetrics().STALE_REQUESTS.add(1);
                    }
                    final long elapsedTime = Time.currentElapsedTime() - request.requestThrottleQueueTime;
                    ServerMetrics.getMetrics().REQUEST_THROTTLE_QUEUE_TIME.add(elapsedTime);
                    if (shouldThrottleOp(request, elapsedTime)) {
                      request.setIsThrottled(true);
                      ServerMetrics.getMetrics().THROTTLED_OPS.add(1);
                    }
                    zks.submitRequestNow(request);
                }
            }
        } catch (InterruptedException e) {
            LOG.error("Unexpected interruption", e);
        }
        int dropped = drainQueue();
        LOG.info("RequestThrottler shutdown. Dropped {} requests", dropped);
    }

    private synchronized void throttleSleep(int stallTime) {
        try {
            ServerMetrics.getMetrics().REQUEST_THROTTLE_WAIT_COUNT.add(1);
            this.wait(stallTime);
        } catch (InterruptedException ie) {
            return;
        }
    }

    @SuppressFBWarnings(value = "NN_NAKED_NOTIFY", justification = "state change is in ZooKeeperServer.decInProgress() ")
    public synchronized void throttleWake() {
        this.notify();
    }

    /**
     * 当限流器正常关闭，队列应该为空。但是如果超过关闭时间限制限流器被关闭，只能放弃
     * 所有请求
     */
    private int drainQueue() {
        // If the throttler shutdown gracefully, the queue will be empty.
        // However, if the shutdown time limit was reached and the throttler
        // was killed, we have no other option than to drop all remaining
        // requests on the floor.
        int dropped = 0;
        Request request;
        LOG.info("Draining request throttler queue");
        while ((request = submittedRequests.poll()) != null) {
            dropped += 1;
            dropRequest(request);
        }
        return dropped;
    }

    /**
     * 因为会话过期而丢弃请求，所以此处直接将连接置为无效，防止之后再有请求进来
     * @param request
     */
    private void dropRequest(Request request) {
        // Since we're dropping a request on the floor, we must mark the
        // connection as invalid to ensure any future requests from this
        // connection are also dropped in order to ensure ordering
        // semantics.
        ServerCnxn conn = request.getConnection();
        if (conn != null) {
            // Note: this will close the connection
            conn.setInvalid();
        }
        // Notify ZooKeeperServer that the request has finished so that it can
        // update any request accounting/throttling limits.
        zks.requestFinished(request);
    }

    public void submitRequest(Request request) {
        if (stopping) {
            LOG.debug("Shutdown in progress. Request cannot be processed");
            dropRequest(request);
        } else {
            request.requestThrottleQueueTime = Time.currentElapsedTime();
            submittedRequests.add(request);
        }
    }

    public int getInflight() {
        return submittedRequests.size();
    }

    public void shutdown() {
        // Try to shutdown gracefully
        LOG.info("Shutting down");
        stopping = true;
        submittedRequests.add(Request.requestOfDeath);
        try {
            this.join(shutdownTimeout);
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting for {} to finish", this);
        }

        // Forcibly shutdown if necessary in order to ensure request
        // queue is drained.
        killed = true;
        try {
            this.join();
        } catch (InterruptedException e) {
            LOG.warn("Interrupted while waiting for {} to finish", this);
            //TODO apply ZOOKEEPER-575 and remove this line.
            ServiceUtils.requestSystemExit(ExitCode.UNEXPECTED_ERROR.getValue());
        }
    }

}
