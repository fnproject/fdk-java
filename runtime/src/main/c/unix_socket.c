
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/errno.h>
#include <sys/un.h>
#include <sys/time.h>
#include <unistd.h>
#include <jni.h>
#include <strings.h>
#include <limits.h>
#include <sys/stat.h>

#ifdef  US_DEBUG
#define debuglog(...) fprintf (stderr, __VA_ARGS__)
#else
#define debuglog(...)

#endif

/**
 * Throws com.fnproject.fn.runtime.ntv.UnixSocetException, adding strerr(errno) as the second arg if that is set
 * @param jenv  java env
 * @param message message to send
 */
void throwIOException(JNIEnv *jenv, const char *message) {
    jclass exc = (*jenv)->FindClass(jenv,
                                    "com/fnproject/fn/runtime/ntv/UnixSocketException");
    if (exc == NULL) { // JVM exception
        return;
    }
    jmethodID constr = (*jenv)->GetMethodID(jenv, exc, "<init>",
                                            "(Ljava/lang/String;Ljava/lang/String;)V");

    if (constr == NULL) { // JVM exception
        return;
    }
    jstring str = (*jenv)->NewStringUTF(jenv, message);
    if (str == NULL) { // JVM exception
        return;
    }
    jstring estr = (*jenv)->NewStringUTF(jenv, strerror(errno));
    if (estr == NULL) { // JVM exception
        return;
    }
    jthrowable t = (jthrowable) (*jenv)->NewObject(jenv, exc, constr, str, estr);
    if (t == NULL) { // JVM exception
        return;
    }
    (*jenv)->Throw(jenv, t);
}


/**
 * Throws a single-arg string exception
 * @param jenv
 * @param clazz  class path (e.g. "java/lang/NullPointerException"
 * @param message  message to pass into constructor
 */
void throwSingleArgStringException(JNIEnv *jenv, const char *clazz, const char *message) {
    jclass exc = (*jenv)->FindClass(jenv, clazz);
    if (exc == NULL) {
        return;
    }
    jmethodID constr = (*jenv)->GetMethodID(jenv, exc, "<init>",
                                            "(Ljava/lang/String;)V");
    if (constr == NULL) { // JVM  exception
        return;
    }
    jstring str = (*jenv)->NewStringUTF(jenv, message);
    if (str == NULL) { // JVM OOM
        return;
    }
    jthrowable t = (jthrowable) (*jenv)->NewObject(jenv, exc, constr, str);
    if (t == NULL) { // JVM error
        return;
    }
    (*jenv)->Throw(jenv, t);
}

void throwIllegalArgumentException(JNIEnv *jenv, const char *message) {
    throwSingleArgStringException(jenv, "java/lang/IllegalArgumentException", message);
}


void throwSocketTimeoutException(JNIEnv *jenv, const char *message) {
    throwSingleArgStringException(jenv, "java/net/SocketTimeoutException", message);
}


void throwNPE(JNIEnv *jenv, const char *message) {
    throwSingleArgStringException(jenv, "java/lang/NullPointerException", message);
}



//     public static native int createSocket();
JNIEXPORT jint JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_socket(JNIEnv *jenv, jclass jClass) {
    errno = 0;
    int rv = socket(PF_UNIX, SOCK_STREAM, 0);

    if (rv == -1) {
        throwIOException(jenv, "Could not create socket");
        return -1;
    }
    debuglog("got result from socket %d\n",rv);

    return rv;
}



//     public static native int bind(int socket, String path)
JNIEXPORT void JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_bind(JNIEnv *jenv, jclass jClass, jint jsocket, jstring jpath) {
    errno = 0;

    struct sockaddr_un addr;
    bzero(&addr, sizeof(struct sockaddr_un));

    addr.sun_family = AF_UNIX;

    const char *nativePath = (*jenv)->GetStringUTFChars(jenv, jpath, 0);
    if (nativePath == NULL) { // JVM OOM
        return;
    }

    if (strlen(nativePath) >= sizeof(addr.sun_path)) {
        (*jenv)->ReleaseStringUTFChars(jenv, jpath, nativePath);
        throwIllegalArgumentException(jenv, "Path too long");
        return;
    }

    strncpy(addr.sun_path, nativePath, sizeof(addr.sun_path));
    (*jenv)->ReleaseStringUTFChars(jenv, jpath, nativePath);

    int rv = bind(jsocket, (struct sockaddr *) &addr, sizeof(addr));
    debuglog("got result from bind %d,%s\n",rv,strerror(errno));
    if (rv < 0) {
        throwIOException(jenv, "Error in bind");
        return;
    }
}

//     public static native void connect(int socket, String path) throws UnixSocketException;
JNIEXPORT void JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_connect(JNIEnv *jenv, jclass jClass, jint jsocket, jstring jpath) {
    errno = 0;


    struct sockaddr_un addr;
    bzero(&addr, sizeof(struct sockaddr_un));
    addr.sun_family = AF_UNIX;

    const char *nativePath = (*jenv)->GetStringUTFChars(jenv, jpath, 0);
    if (nativePath == NULL) {// JVM OOM
        return;
    }

    if (strlen(nativePath) >= sizeof(addr.sun_path)) {
        (*jenv)->ReleaseStringUTFChars(jenv, jpath, nativePath);
        throwIllegalArgumentException(jenv, "Path too long");
        return;
    }

    strncpy(addr.sun_path, nativePath, sizeof(addr.sun_path));
    (*jenv)->ReleaseStringUTFChars(jenv, jpath, nativePath);
    int result = connect(jsocket, (struct sockaddr *) &addr, sizeof(addr));
    debuglog("%d: got result from connect %d %s\n",jsocket,result,strerror(errno));
    if (result < 0) {
        if (errno == ETIMEDOUT) {
            throwSocketTimeoutException(jenv, "Socket connect timed out");
            return;
        }
        throwIOException(jenv, "Error in connect");
        return;
    }
}

//     public static native void listen(int socket, int backlog);
JNIEXPORT void JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_listen(JNIEnv *jenv, jclass jClass, jint jsocket, jint jbacklog) {
    errno = 0;

    int rv = listen(jsocket, jbacklog);
    debuglog("got result from listen %d,%s\n",rv,strerror(errno));

    if (rv < 0) {
        throwIOException(jenv, "Error in listen");
        return;
    }
}



//    public static native int accept(int socket, long timeoutMs) throws UnixSocketException;
// returns 0 in case that the accept timed out
JNIEXPORT jint JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_accept(JNIEnv *jenv, jclass jClass, jint jsocket, jlong timeoutMs) {
    errno = 0;

    if (timeoutMs < 0) {
        throwIllegalArgumentException(jenv, "Invalid timeout");
        return -1;
    }

    struct timeval startTime;

    if (gettimeofday(&startTime, NULL) < 0) {
        throwIOException(jenv, "Failed to get time");
        return -1;
    }

    struct timeval timeoutAbs;
    timeoutAbs.tv_sec = timeoutMs / 1000;
    timeoutAbs.tv_usec = (int) (timeoutMs % 1000) * 1000;


    int rv;

    struct timeval *toPtr = NULL;
    struct timeval actualTo;


    do {
        errno = 0;

        if (timeoutMs > 0) {
            struct timeval nowTime, used;
            if (gettimeofday(&nowTime, NULL) < 0) {
                throwIOException(jenv, "Failed to get time");
                return -1;
            }

            timersub(&nowTime, &startTime, &used);
            timersub(&timeoutAbs, &used, &actualTo);
            if (actualTo.tv_sec < 0 || (actualTo.tv_sec == 0 && actualTo.tv_usec ==0) ) {
                // hit end of poll in loop
                return 0;

            }
            toPtr = &actualTo;
        }

        fd_set set;
        FD_ZERO(&set); /* clear the set */
        FD_SET(jsocket, &set); /* add our file descriptor to the set */

        rv = select(jsocket + 1, &set, NULL, NULL, toPtr);
        debuglog("XXX %d Got result from select %d : %s\n",jsocket,rv,strerror(errno));

        if(!FD_ISSET(jsocket,&set)){
            continue;
        }
    } while (rv == -1 && errno == EINTR);

    if (rv < 0) {
        throwIOException(jenv, "Error in select");
        return -1;
    } else if (rv == 0) {
        return 0; // timeout
    }


    int result;
    do {
        struct sockaddr_un addr;
        bzero(&addr, sizeof(struct sockaddr_un));
        socklen_t rlen = sizeof(struct sockaddr_un);
        result = accept(jsocket, (struct sockaddr *) &addr, &rlen);
        debuglog("XXX %d Got result from accept %d : %s\n",jsocket, result,strerror(errno));

    } while (result == -1 && errno == EINTR);

    if (result < 0) {
        throwIOException(jenv, "Error in accept");
    }
    return result;
}



//    public static native int recv(int socket, byte[] buffer, jint offset, jint length) throws UnixSocketException;
JNIEXPORT jint JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_recv(JNIEnv *jenv, jclass jClass, jint jsocket, jbyteArray jbuf,
                                                        jint offset, jint length) {
    errno = 0;

    if (offset < 0 || length <= 0) {
        throwIllegalArgumentException(jenv, "Invalid offset, length");
        return -1;
    }

    if (jbuf == NULL) {
        throwNPE(jenv, "buffer is null");
        return -1;
    }

    jint bufLen = (*jenv)->GetArrayLength(jenv, jbuf);

    if (offset >= bufLen) {
        throwIllegalArgumentException(jenv, "Invalid offset, beyond end of buffer");
        return -1;
    }
    if (length > (bufLen - offset)) {
        length = bufLen - offset;
    }


    jbyte *buf = (*jenv)->GetByteArrayElements(jenv, jbuf, NULL);
    if (buf == NULL) {
        return -1;
    }


    ssize_t rcount;

    do {
        rcount = read(jsocket, &(buf[offset]), (size_t) length);
        debuglog("XXX %d Got result from read %ld : %s\n",jsocket,rcount,strerror(errno));

    } while (rcount == -1 && errno == EINTR);

    (*jenv)->ReleaseByteArrayElements(jenv, jbuf, buf, 0);


    if (rcount == 0) {
        // EOF in c is -1 in java
        return -1;
    } else if (rcount < 0) {
        if (errno == EAGAIN) {
            throwSocketTimeoutException(jenv, "Timeout reading from socket");
            return -1;
        }
        throwIOException(jenv, "Error reading from socket");
        return -1;
    }
    return (jint) rcount;

}


//     public static native int send(int socket, byte[] buffer) throws UnixSocketException;
JNIEXPORT jint JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_send(JNIEnv *jenv, jclass jClass, jint jsocket, jbyteArray jbuf,
                                                        jint offset, jint length) {
    errno = 0;

    if (offset < 0 || length <= 0) {
        throwIllegalArgumentException(jenv, "Invalid offset, length or timeout");
        return -1;
    }

    if (jbuf == NULL) {
        throwNPE(jenv, "buffer is null");
        return -1;
    }

    jint bufLen = (*jenv)->GetArrayLength(jenv, jbuf);


    if ((offset >= bufLen) || (length > (bufLen - offset))) {
        throwIllegalArgumentException(jenv, "Invalid offset or length, beyond end of buffer");
        return -1;
    }


    jbyte *buf = (*jenv)->GetByteArrayElements(jenv, jbuf, NULL);
    if (buf == NULL) { // JVM OOM
        return -1;
    }
    ssize_t wcount;
    do {
        wcount = write(jsocket, &(buf[offset]), (size_t) length);
        debuglog("XXX %d Got result from write %ld : %s\n",jsocket,wcount,strerror(errno));
    } while (wcount == -1 && errno == EINTR);


    (*jenv)->ReleaseByteArrayElements(jenv, jbuf, buf, 0);

    if (wcount < 0) {
        if (errno == EAGAIN) {
            throwSocketTimeoutException(jenv, "Timeout writing to socket");
            return -1;
        }
        throwIOException(jenv, "Error reading from socket");
        return -1;
    }

    return (jint) wcount;

}




//     public static native  close(int socket);

JNIEXPORT void JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_close(JNIEnv *jenv, jclass jClass, jint jsocket) {
    errno = 0;

    int rv = close(jsocket);
    debuglog("XXX %d got result from close %d,%s\n",jsocket,rv,strerror(errno));
    if (rv < 0) {
        throwIOException(jenv, "Error in closing socket");
        return;
    }
}


JNIEXPORT void JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_setSendBufSize(JNIEnv *jenv, jclass jClass, jint socket,
                                                                  jint bufsize) {
    errno = 0;
    if (bufsize <= 0) {
        throwIllegalArgumentException(jenv, "Invalid buffer size");
        return;
    }

    int rv = setsockopt(socket, SOL_SOCKET, SO_SNDBUF, &bufsize, sizeof(jint));
    if (rv < 0) {
        throwIOException(jenv, "Error setting socket options");
        return;
    }
}

JNIEXPORT void JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_setRecvBufSize(JNIEnv *jenv, jclass jClass, jint socket,
                                                                  jint bufsize) {
    errno = 0;
    if (bufsize <= 0) {
        throwIllegalArgumentException(jenv, "invalid buffer size");
        return;
    }

    int rv = setsockopt(socket, SOL_SOCKET, SO_RCVBUF, &bufsize, sizeof(jint));
    if (rv < 0) {
        throwIOException(jenv, "Error setting socket options");
        return;
    }
}

JNIEXPORT void JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_setSendTimeout(JNIEnv *jenv, jclass jClass, jint socket,
                                                                  jint timeout) {
    errno = 0;
    if (timeout < 0) {
        throwIllegalArgumentException(jenv, "invalid timeout");
        return;
    }

    struct timeval tv;
    tv.tv_sec = timeout / 1000;
    tv.tv_usec = (timeout % 1000) * 1000;

    int rv = setsockopt(socket, SOL_SOCKET, SO_SNDTIMEO, &tv, sizeof(struct timeval));
    if (rv < 0) {
        throwIOException(jenv, "Error setting socket options");
        return;
    }
}


JNIEXPORT jint JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_getSendTimeout(JNIEnv *jenv, jclass jClass, jint socket) {
    errno = 0;

    struct timeval tv;
    bzero(&tv, sizeof(struct timeval));
    socklen_t len = sizeof(struct timeval);

    int rv = getsockopt(socket, SOL_SOCKET, SO_SNDTIMEO, &tv, &len);
    debuglog("XXX %d getsockopt _getSendTimeout  rv %dz\n",socket,rv);

    if (rv < 0) {
        throwIOException(jenv, "Error setting socket options");
        return -1;
    }
    time_t msecs = tv.tv_sec * 1000 + tv.tv_usec / 1000;
    if (msecs > INT_MAX) {
        return (jint) INT_MAX;
    }
    return (jint) msecs;
}


JNIEXPORT void JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_setRecvTimeout(JNIEnv *jenv, jclass jClass, jint socket,
                                                                  jint timeout) {
    errno = 0;
    if (timeout < 0) {
        throwIllegalArgumentException(jenv, "Invalid timeout");
        return;
    }

    struct timeval tv;
    tv.tv_sec = timeout / 1000;
    tv.tv_usec = (timeout % 1000) * 1000;

    int rv = setsockopt(socket, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(struct timeval));
    debuglog("XXX %d setsockopt setRecvTimeout rv %dz\n",socket,rv);
    if (rv < 0) {
        throwIOException(jenv, "Error setting socket options");
        return;
    }
}


JNIEXPORT jint JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_getRecvTimeout(JNIEnv *jenv, jclass jClass, jint socket) {
    errno = 0;

    struct timeval tv;
    bzero(&tv, sizeof(struct timeval));
    socklen_t len = sizeof(struct timeval);

    int rv = getsockopt(socket, SOL_SOCKET, SO_RCVTIMEO, &tv, &len);
    debuglog("XXX %d getsockopt rv %dz\n",socket,rv);

    if (rv < 0) {
        throwIOException(jenv, "Error setting socket options");
        return -1;
    }

    debuglog("XXX %d getsockopt _getSendTimeout  rv %dz\n",socket,rv);

    time_t msecs = tv.tv_sec * 1000 + tv.tv_usec / 1000;
    if (msecs > INT_MAX) {
        return (jint) INT_MAX;
    }
    return (jint) msecs;
}


// public static native void shutdown(int socket, boolean input, boolean output) ;
JNIEXPORT void JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_shutdown(JNIEnv *jenv, jclass jClass, jint jsocket, jboolean input,
                                                            jboolean output) {
    errno = 0;
    int how;

    if (input && output) {
        how = SHUT_RDWR;
    } else if (input) {
        how = SHUT_RD;
    } else if (output) {
        how = SHUT_WR;
    } else {
        return;
    }


    int rv = shutdown(jsocket, how);
    debuglog("XXX %d got result from shutdown %d  %d,%s\n",jsocket,how,rv,strerror(errno));
    if (rv < 0) {
        throwIOException(jenv, "Failed to shut down socket ");
        return;
    }
}
