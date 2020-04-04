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

/**
 * 请求处理器被连接在一起来处理事务。请求按顺序处理。独立服务器、follower、leader 都有一点
 * 不同。
 * 请求通过processRequest() 来提交至请求处理器。通常，方法是单线程调用的。
 * 当shutdown 被调用时，请求处理器需要关闭它连接的所有请求处理器
 *
 * RequestProcessors are chained together to process transactions. Requests are
 * always processed in order. The standalone server, follower, and leader all
 * have slightly different RequestProcessors chained together.
 *
 * Requests always move forward through the chain of RequestProcessors. Requests
 * are passed to a RequestProcessor through processRequest(). Generally method
 * will always be invoked by a single thread.
 *
 * When shutdown is called, the request RequestProcessor should also shutdown
 * any RequestProcessors that it is connected to.
 */
public interface RequestProcessor {

    @SuppressWarnings("serial")
    class RequestProcessorException extends Exception {

        public RequestProcessorException(String msg, Throwable t) {
            super(msg, t);
        }

    }

    void processRequest(Request request) throws RequestProcessorException;

    void shutdown();

}
