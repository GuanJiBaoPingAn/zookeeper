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

package org.apache.zookeeper;

import java.util.List;
import org.apache.yetus.audience.InterfaceAudience;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Stat;

/**
 * 定义异步回调的接口
 * <p>ZooKeeper 对同步和异步提供一样的API
 * <p>异步回调是在方法返回后进行调用
 * <p>
 * Interface definitions of asynchronous callbacks.
 *
 * <p>ZooKeeper provides asynchronous version as equivalent to synchronous APIs.
 *
 * <p>An asynchronous callback is deferred to invoke after a function returns.
 * Asynchronous calls usually improve system efficiency on IO-related APIs.
 *
 * <p>It is highly recommended NOT to perform any blocking operation inside
 * the callbacks. If you block the thread the ZooKeeper client won't process
 * other events.
 */
@InterfaceAudience.Public
public interface AsyncCallback {

    /**
     * 该回调用于获取node 的状态
     * This callback is used to retrieve the stat of the node.
     */
    @InterfaceAudience.Public
    interface StatCallback extends AsyncCallback {

        /**
         * 处理异步调用结果
         * <p>成功，rc为{@link KeeperException.Code#OK}
         * <p>失败，rc为相应错误码{@link KeeperException}
         * <ul>
         *     <li>{@link KeeperException.Code#NONODE} 给定路径节点不存在
         *     <li>{@link KeeperException.Code#BADVERSION} 给定版本不符合节点版本
         * </ul>
         * Process the result of the asynchronous call.
         *
         * <p>On success, rc is {@link KeeperException.Code#OK}.
         *
         * <p>On failure, rc is set to the corresponding failure code in {@link KeeperException}.
         * <ul>
         *  <li> {@link KeeperException.Code#NONODE}
         *              - The node on given path doesn't exist for some API calls.</li>
         *  <li> {@link KeeperException.Code#BADVERSION}
         *              - The given version doesn't match the node's version for some API calls.</li>
         * </ul>
         *
         * @param rc   The return code or the result of the call.
         * @param path The path that we passed to asynchronous calls.
         * @param ctx  Whatever context object that we passed to asynchronous calls.
         * @param stat {@link Stat} object of the node on given path.
         *
         * @see ZooKeeper#exists(String, boolean, StatCallback, Object)
         * @see ZooKeeper#exists(String, Watcher, StatCallback, Object)
         * @see ZooKeeper#setData(String, byte[], int, StatCallback, Object)
         * @see ZooKeeper#setACL(String, List, int, StatCallback, Object)
         */
        void processResult(int rc, String path, Object ctx, Stat stat);

    }

    /**
     * 用于获取给定路径节点的子节点个数回调
     * This callback is used to get all children node number of the node.
     *
     * @since 3.6.0
     */
    @InterfaceAudience.Public
    interface AllChildrenNumberCallback extends AsyncCallback {

        /**
         * @param rc      The return code or the result of the call.
         * @param ctx     Whatever context object that we passed to asynchronous calls.
         * @param number  The number of children nodes under a specific path.
         *
         * @see ZooKeeper#getAllChildrenNumber(String, AllChildrenNumberCallback, Object)
         */
        void processResult(int rc, String path, Object ctx, int number);

    }

    /**
     * 用于获取给定节点的数据和状态
     * This callback is used to retrieve the data and stat of the node.
     */
    @InterfaceAudience.Public
    interface DataCallback extends AsyncCallback {

        /**
         * Process the result of asynchronous calls.
         *
         * <p>On success, rc is {@link KeeperException.Code#OK}.
         *
         * <p>On failure, rc is set to the corresponding failure code in {@link KeeperException}.
         * <ul>
         *  <li>{@link KeeperException.Code#NONODE}
         *             - The node on given path doesn't exist for some API calls.</li>
         * </ul>
         *
         * @param rc   The return code or the result of the call.
         * @param path The path that we passed to asynchronous calls.
         * @param ctx  Whatever context object that we passed to asynchronous calls.
         * @param data The data of the node.
         * @param stat {@link Stat} object of the node on given path.
         *
         * @see ZooKeeper#getData(String, boolean, DataCallback, Object)
         * @see ZooKeeper#getData(String, Watcher, DataCallback, Object)
         * @see ZooKeeper#getConfig(boolean, DataCallback, Object)
         * @see ZooKeeper#getConfig(Watcher, DataCallback, Object)
         */
        void processResult(int rc, String path, Object ctx, byte[] data, Stat stat);

    }

    /**
     * 获取节点的ACL 和状态
     * This callback is used to retrieve the ACL and stat of the node.
     */
    @InterfaceAudience.Public
    interface ACLCallback extends AsyncCallback {

        /**
         * Process the result of the asynchronous call.
         *
         * <p>On success, rc is {@link KeeperException.Code#OK}.
         *
         * <p>On failure, rc is set to the corresponding failure code in {@link KeeperException}.
         * <ul>
         *  <li>{@link KeeperException.Code#NONODE}
         *             - The node on given path doesn't exist for some API calls.</li>
         * </ul>
         *
         * @param rc   The return code or the result of the call.
         * @param path The path that we passed to asynchronous calls.
         * @param ctx  Whatever context object that we passed to asynchronous calls.
         * @param acl  ACL Id in {@link ZooDefs.Ids}.
         * @param stat {@link Stat} object of the node on given path.
         *
         * @see ZooKeeper#getACL(String, Stat, ACLCallback, Object)
         */
        void processResult(int rc, String path, Object ctx, List<ACL> acl, Stat stat);

    }

    /**
     * 获取节点的子节点
     * This callback is used to retrieve the children of the node.
     */
    @InterfaceAudience.Public
    interface ChildrenCallback extends AsyncCallback {

        /**
         * Process the result of the asynchronous call.
         *
         * <p>On success, rc is {@link KeeperException.Code#OK}.
         *
         * <p>On failure, rc is set to the corresponding failure code in {@link KeeperException}.
         * <ul>
         *  <li>{@link KeeperException.Code#NONODE}
         *             - The node on given path doesn't exist for some API calls.</li>
         * </ul>
         *
         * @param rc       The return code or the result of the call.
         * @param path     The path that we passed to asynchronous calls.
         * @param ctx      Whatever context object that we passed to asynchronous calls.
         * @param children An unordered array of children of the node on given path.
         *
         * @see ZooKeeper#getChildren(String, boolean, ChildrenCallback, Object)
         * @see ZooKeeper#getChildren(String, Watcher, ChildrenCallback, Object)
         */
        void processResult(int rc, String path, Object ctx, List<String> children);

    }

    /**
     * 用于获取给定节点的子节点和状态
     * This callback is used to retrieve the children and stat of the node.
     */
    @InterfaceAudience.Public
    interface Children2Callback extends AsyncCallback {

        /**
         * Process the result of the asynchronous call.
         *
         * @param rc       The return code or the result of the call.
         * @param path     The path that we passed to asynchronous calls.
         * @param ctx      Whatever context object that we passed to asynchronous calls.
         * @param children An unordered array of children of the node on given path.
         * @param stat     {@link Stat} object of the node on given path.
         *
         * @see ChildrenCallback
         * @see ZooKeeper#getChildren(String, boolean, Children2Callback, Object)
         * @see ZooKeeper#getChildren(String, Watcher, Children2Callback, Object)
         */
        void processResult(int rc, String path, Object ctx, List<String> children, Stat stat);

    }

    /**
     * 用于获取及诶单的名称和状态
     * This callback is used to retrieve the name and stat of the node.
     */
    @InterfaceAudience.Public
    interface Create2Callback extends AsyncCallback {

        /**
         * Process the result of the asynchronous call.
         *
         * @param rc   The return code or the result of the call.
         * @param path The path that we passed to asynchronous calls.
         * @param ctx  Whatever context object that we passed to asynchronous calls.
         * @param name The name of the Znode that was created. On success, <i>name</i>
         *             and <i>path</i> are usually equal, unless a sequential node has
         *             been created.
         * @param stat {@link Stat} object of the node on given path.
         *
         * @see StringCallback
         * @see ZooKeeper#create(String, byte[], List, CreateMode, Create2Callback, Object)
         * @see ZooKeeper#create(String, byte[], List, CreateMode, Create2Callback, Object, long)
         */
        void processResult(int rc, String path, Object ctx, String name, Stat stat);

    }

    /**
     * 用于获取节点的名称
     * This callback is used to retrieve the name of the node.
     */
    @InterfaceAudience.Public
    interface StringCallback extends AsyncCallback {

        /**
         * Process the result of the asynchronous call.
         *
         * <p>On success, rc is {@link KeeperException.Code#OK}.
         *
         * <p>On failure, rc is set to the corresponding failure code in {@link KeeperException}.
         * <ul>
         *  <li>{@link KeeperException.Code#NODEEXISTS}
         *             - The node on give path already exists for some API calls.</li>
         *  <li>{@link KeeperException.Code#NONODE}
         *             - The node on given path doesn't exist for some API calls.</li>
         *  <li>{@link KeeperException.Code#NOCHILDRENFOREPHEMERALS}
         *             - An ephemeral node cannot have children. There is discussion in
         *             community. It might be changed in the future.</li>
         * </ul>
         *
         * @param rc   The return code or the result of the call.
         * @param path The path that we passed to asynchronous calls.
         * @param ctx  Whatever context object that we passed to asynchronous calls.
         * @param name The name of the znode that was created. On success, <i>name</i>
         *             and <i>path</i> are usually equal, unless a sequential node has
         *             been created.
         *
         * @see ZooKeeper#create(String, byte[], List, CreateMode, StringCallback, Object)
         */
        void processResult(int rc, String path, Object ctx, String name);

    }

    /**
     * 该回调不获取节点的任何数据。
     * This callback doesn't retrieve anything from the node. It is useful for some APIs
     * that doesn't want anything sent back, e.g. {@link ZooKeeper#sync(String, VoidCallback, Object)}.
     */
    @InterfaceAudience.Public
    interface VoidCallback extends AsyncCallback {

        /**
         * Process the result of the asynchronous call.
         *
         * <p>On success, rc is {@link KeeperException.Code#OK}.
         *
         * <p>On failure, rc is set to the corresponding failure code in {@link KeeperException}.
         * <ul>
         *  <li>{@link KeeperException.Code#NONODE}
         *             - The node on given path doesn't exist for some API calls.</li>
         *  <li>{@link KeeperException.Code#BADVERSION}
         *             - The given version doesn't match the node's version for some API calls.</li>
         *  <li>{@link KeeperException.Code#NOTEMPTY}
         *             - the node has children and some API calls cannot succeed, e.g.
         *             {@link ZooKeeper#delete(String, int, VoidCallback, Object)}.</li>
         * </ul>
         *
         * @param rc   The return code or the result of the call.
         * @param path The path that we passed to asynchronous calls.
         * @param ctx  Whatever context object that we passed to asynchronous calls.
         *
         * @see ZooKeeper#delete(String, int, VoidCallback, Object)
         * @see ZooKeeper#removeAllWatches(String, Watcher.WatcherType, boolean, VoidCallback, Object)
         * @see ZooKeeper#removeWatches(String, Watcher, Watcher.WatcherType, boolean, VoidCallback, Object)
         * @see ZooKeeper#sync(String, VoidCallback, Object)
         *
         */
        void processResult(int rc, String path, Object ctx);

    }

    /**
     * 用于处理一个多次调用的多个结果
     * This callback is used to process the multiple results from a single multi call.
     */
    @InterfaceAudience.Public
    interface MultiCallback extends AsyncCallback {

        /**
         * Process the result of the asynchronous call.
         *
         * <p>On success, rc is {@link KeeperException.Code#OK}. All {@code opResults} are
         * non-{@link OpResult.ErrorResult}.
         *
         * <p>On failure, rc is a failure code in {@link KeeperException.Code}. Either
         * {@code opResults} is null, or all {@code opResults} are {@link OpResult.ErrorResult}.
         * All operations will be rolled back even if operations before the failing one were
         * successful.
         *
         * @param rc   The return code or the result of the call.
         * @param path The path that we passed to asynchronous calls.
         * @param ctx  Whatever context object that we passed to asynchronous calls.
         * @param opResults The list of results. One result for each operation, and the order
         *                  matches that of input.
         *
         * @see ZooKeeper#multi(Iterable, MultiCallback, Object)
         */
        void processResult(int rc, String path, Object ctx, List<OpResult> opResults);

    }

    /**
     * 用于处理一个getEphemeral 调用的结果
     * This callback is used to process the getEphemerals results from a single getEphemerals call.
     *
     * @see ZooKeeper#getEphemerals(EphemeralsCallback, Object)
     * @see ZooKeeper#getEphemerals(String, EphemeralsCallback, Object)
     *
     * @since 3.6.0
     */
    interface EphemeralsCallback extends AsyncCallback {

        /**
         * @param rc      The return code or the result of the call.
         * @param ctx     Whatever context object that we passed to asynchronous calls.
         * @param paths   The path that we passed to asynchronous calls.
         */
        void processResult(int rc, Object ctx, List<String> paths);

    }

}
