package com.hmdp.utils;

public interface ILock {
    boolean tryLock(Long timeSec);
    void unlock();

}
