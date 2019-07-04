class CustomLock {

    abstract class Sync extends AbstractQueuedSynchronizer {

        abstract void lock();


        protected boolean nonfairTryAcquire(int arg) {
            /**
             * @Author myou<myoueva@gmail.com>
             * @Description
             * 1.获取volatile变量state的值，abs中state代表锁的标识，加锁和释放都是操作state进行增加和减少，此变量可以模拟共享和独占
             * 2.判断state是否为0，如果为0则没有线程操作该变量，当前线程即可获得锁，进行compareAndSet操作为了防止在判断过程中有线程争抢state赋值
             * 3.cas操作成功，则当前线程获取锁标识，返回结果并将线程对象植入abstractOwnerThread对象属性中
             * 4.若cas设置失败或者state不等于0，则拿到拥有锁的线程与当前线程对比，若一致则认为可重入进行state重入操作
             * 5.以上都不满足则获取失败
             * @Date 10:24 PM 2019/7/4
             * @Param [arg]
             * @return boolean
             **/
            int state = getState();
            if (state==0){
                if (compareAndSetState(0, arg)){
                    setExclusiveOwnerThread(Thread.currentThread());
                    return true;
                }
            }else if (getExclusiveOwnerThread()==Thread.currentThread()){
                setState(state+arg);
                return true;
            }
            return false;
        }

        @Override
        protected boolean tryRelease(int arg) {
            /**
             * @Author myou<myoueva@gmail.com>
             * @Description
             * 1.判断执行释放线程是否为锁的拥有线程，否则返回释放失败结果
             * 2.锁的释放就是指定同一个线程才能去进行释放state递减操作，所以不需要考虑在判断state=0时其他线程加入
             * 3.如果递减为0，则该线程才算完全释放锁
             * @Date 10:38 PM 2019/7/4
             * @Param [arg]
             * @return boolean
             **/
            int state = getState();
            if (getExclusiveOwnerThread()==Thread.currentThread()){
                return false;
            }
            int q = state-1;
            boolean free = false;
            if (q==0){
                setExclusiveOwnerThread(null);
                free = true;
                return free;
            }
            setState(q);
            return free;
        }

        @Override
        protected boolean isHeldExclusively() {
            return getExclusiveOwnerThread() == Thread.currentThread();
        }
    }

    class NonfairSync extends Sync{

        @Override
        protected boolean tryAcquire(int arg) {
            return nonfairTryAcquire(arg);
        }

        @Override
        void lock() {
            if (compareAndSetState(0, 1)){
                setExclusiveOwnerThread(Thread.currentThread());
            }else {
                acquire(1);
            }
        }
    }

    class fairSync extends Sync{

        @Override
        protected boolean tryAcquire(int arg) {
            /**
             * @Author myou<myoueva@gmail.com>
             * @Description //
             * 公平锁与非公平锁多一步链表head判断即先判断当前节点是否排在链表第一个要获取锁的节点
             * @Date 10:53 PM 2019/7/4
             * @Param [arg]
             * @return boolean
             **/
            int state = getState();
            if (state==0){
                if (!hasQueuedPredecessors()&&compareAndSetState(0, arg)){
                    setExclusiveOwnerThread(Thread.currentThread());
                    return true;
                }
            }else if (getExclusiveQueuedThreads()==Thread.currentThread()){
                setState(state+arg);
                return true;
            }
            return false;
        }

        @Override
        void lock() {
            acquire(1);
        }
    }

}
