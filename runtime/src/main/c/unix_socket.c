
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/errno.h>
#include <sys/un.h>
#include <unistd.h>
#include <jni.h>
#include <strings.h>
#include <limits.h>
#include <sys/stat.h>

/**
 * Throws an IO exception adding via UnixSocketException , adding strerr(errno) if that is set
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

    if (!rv) {
        throwIOException(jenv, "Could not create socket");
        return -1;
    }

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

    if (bind(jsocket, (struct sockaddr *) &addr, sizeof(addr)) < 0) {
        throwIOException(jenv, "error in bind");
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

    if (connect(jsocket, (struct sockaddr *) &addr, sizeof(addr)) < 0) {
        if (errno == ETIMEDOUT) {
            throwSocketTimeoutException(jenv, "Socket connect timed out");
            return;
        }
        throwIOException(jenv, "error in bind");
        return;
    }
}

//     public static native void listen(int socket, int backlog);
JNIEXPORT void JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_listen(JNIEnv *jenv, jclass jClass, jint jsocket, jint jbacklog) {
    errno = 0;

    if (listen(jsocket, jbacklog) < 0) {
        throwIOException(jenv, "error in listen");
        return;
    }
}



//    public static native int accept(int socket, long timeoutMs) throws UnixSocketException;
// returns 0 in case that the accept timed out
JNIEXPORT jint JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_accept(JNIEnv *jenv, jclass jClass, jint jsocket, jlong timeoutMs) {
    errno = 0;

    struct sockaddr_un addr;
    bzero(&addr, sizeof(struct sockaddr_un));
    socklen_t rlen;

    fd_set set;
    struct timeval timeout;

    struct timeval *to_ptr = NULL;
    if (timeoutMs > 0) {
        timeout.tv_sec = timeoutMs / 1000;
        timeout.tv_usec = (int) (timeoutMs % 1000) * 1000;
        to_ptr = &timeout;
    }

    FD_ZERO(&set); /* clear the set */
    FD_SET(jsocket, &set); /* add our file descriptor to the set */

    int rv;
    rv = select(jsocket + 1, &set, NULL, NULL, to_ptr);
    if (rv < 0) {
        throwIOException(jenv, "error in select");
        return -1;
    } else if (rv == 0) {
        return 0; // timeout
    }
    int result = accept(jsocket, (struct sockaddr *) &addr, &rlen);
    if (result < 0) {
        throwIOException(jenv, "error in accept");
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
        throwIllegalArgumentException(jenv, "Invalid offset , beyond end of buffer");
        return -1;
    }
    if (length > (bufLen - offset)) {
        length = bufLen - offset;
    }


    jbyte *buf = (*jenv)->GetByteArrayElements(jenv, jbuf, NULL);
    if (buf == NULL) {
        return -1;
    }


    ssize_t rcount = read(jsocket, &(buf[offset]), (size_t) length);
    (*jenv)->ReleaseByteArrayElements(jenv, jbuf, buf, 0);


    if (rcount == 0) {
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
        throwIllegalArgumentException(jenv, "Invalid offset,  length or timeout");
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


    ssize_t wcount = write(jsocket, &(buf[offset]), (size_t) length);
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

    if (close(jsocket) < 0) {
        throwIOException(jenv, "Error in closing socket");
        return;
    }
}


JNIEXPORT void JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_setSendBufSize(JNIEnv *jenv, jclass jClass, jint socket,
                                                                  jint bufsize) {
    errno = 0;
    if (bufsize <= 0) {
        throwIllegalArgumentException(jenv, "invalid buffer size");
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
        throwIllegalArgumentException(jenv, "invalid buffer size");
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
    socklen_t len;

    int rv = getsockopt(socket, SOL_SOCKET, SO_SNDTIMEO, &tv, &len);
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
        throwIllegalArgumentException(jenv, "invalid buffer size");
        return;
    }

    struct timeval tv;
    tv.tv_sec = timeout / 1000;
    tv.tv_usec = (timeout % 1000) * 1000;

    int rv = setsockopt(socket, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(struct timeval));
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
    socklen_t len;

    int rv = getsockopt(socket, SOL_SOCKET, SO_RCVTIMEO, &tv, &len);
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


// public static native void shutdown(int socket, boolean input, boolean output) ;
JNIEXPORT void JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_shutdown(JNIEnv *jenv, jclass jClass, jint socket, jboolean input,
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

    int rv = shutdown(socket, how);
    if (rv < 0) {
        throwIOException(jenv, "faield to shut down socket ");
        return;
    }
}
