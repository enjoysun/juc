import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

class MscLock {
    private static class Node {
        /**
         * @Author myou<myoueva@gmail.com>
         * @Description //链表结构
         *          *       +------+  next +-----+       +-----+
         *          *  head |      | <---- |     | <---- |     |  tail
         *          *       +------+       +-----+       +-----+
         * @Date 10:35 AM 2019/7/4
         * @Param
         * @return
         **/
        public volatile boolean lock; //false释放锁上一个节点可获取锁标识，true需要获得锁
        public Thread lockThread;
        public Node next;

        public Node(boolean isWait, Thread thread, Node next) {
            this.lock = isWait;
            this.next = next;
            this.lockThread = thread;
        }
    }

    private static volatile AtomicReference<Node> tail = new AtomicReference<Node>();

    public boolean lock() {
        /**
         * @Author myou<myoueva@gmail.com>
         * @Description 自旋上锁
         * 构造当前线程Node节点，执行mcs队列getAndSet操作，获取操作结果result
         * 如result==null则证明tail位置没有节点，该Node可立刻获得锁，设置Node的lock属性为false
         * 如result!=null则证明该链表有前置节点，将要做的是将前置节点指向Node的next，tail尾节点即当前Node
         * 自旋监控上一个节点，是否释放锁，若状态为需要锁，则进入自旋，反之结束自旋获取锁(锁状态标识使用volatile语言，一写多读机制，将自旋控制在本线程的独立副本中计算)
         * @Date 10:01 AM 2019/7/4
         * @Param []
         * @return boolean
         **/
        Node curNode = new Node(true, Thread.currentThread(), null);
        Node pro = tail.getAndSet(curNode);
        if (pro == null) {
            tail.get().lock = false;
            System.out.println("线程:"+Thread.currentThread().getName()+"获取锁");
            return true;
        } else {
            tail.get().next = pro;
            for (; ; ) {
                if (pro.lock) {
                    System.out.println("线程:"+Thread.currentThread().getName()+"进入自旋");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }else {
                    System.out.println("自旋结束"+Thread.currentThread().getName()+"获取锁");
                    return true;
                }
            }
        }
    }

    public boolean release() {
        /**
         * @Author myou<myoueva@gmail.com>
         * @Description //自旋释放
         * 获取tail的当前节点node
         * node.next==null证明该链表只有一个元素，可以进行node的锁标识设置false进行释放，尝试将tail设置为null
         * node.next!=true需要设置node的下一个元素的lock为释放状态
         * @Date 10:26 AM 2019/7/4
         * @Param []
         * @return boolean
         **/
        for (; ; ) {
            Node cur = tail.get();
            if (cur.next == null) {
                    while (tail.compareAndSet(cur, null)) {
                        System.out.println("线程:" + Thread.currentThread().getName() + "释放成功");
                        return true;
                }
            }
            cur.next.lock = false; //取消即将拥有线程节点的自旋条件
            cur.next = null;
            System.out.println("线程:" + Thread.currentThread().getName() + "释放成功");
            return true;
        }
    }
}
