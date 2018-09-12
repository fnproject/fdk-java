
#include <sys/socket.h>
#include <sys/types.h>
#include <sys/errno.h>
#include <sys/un.h>
#include <unistd.h>
#include <jni.h>
#include <strings.h>

void throwIOException(JNIEnv *jenv, const char *message) {
    jclass exc = (*jenv)->FindClass(jenv,
                                    "com/fnproject/fn/runtime/ntv/UnixSocketException");
    jmethodID constr = (*jenv)->GetMethodID(jenv, exc, "<init>",
                                            "(Ljava/lang/String;Ljava/lang/String;)V");

    jstring str = (*jenv)->NewStringUTF(jenv, message);
    jstring estr = (*jenv)->NewStringUTF(jenv, strerror(errno));
    jthrowable t = (jthrowable) (*jenv)->NewObject(jenv, exc, constr, str, estr);
    (*jenv)->Throw(jenv, t);
}


void throwIllegalArgumentException(JNIEnv *jenv, const char *message) {

    jclass exc = (*jenv)->FindClass(jenv,
                                    "java/lang/IllegalArgumentException");
    jmethodID constr = (*jenv)->GetMethodID(jenv, exc, "<init>",
                                            "(Ljava/lang/String;)V");
    jstring str = (*jenv)->NewStringUTF(jenv, message);
    jthrowable t = (jthrowable) (*jenv)->NewObject(jenv, exc, constr, str);
    (*jenv)->Throw(jenv, t);
}


//     public static native int createSocket();
JNIEXPORT jint JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_socket(JNIEnv *jenv, jclass jClass) {
    return socket(PF_UNIX, SOCK_STREAM, 0);
}


//     public static native int bind(int socket, String path)
JNIEXPORT void JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_bind(JNIEnv *jenv, jclass jClass, jint jsocket, jstring jpath) {
    struct sockaddr_un addr;
    bzero(&addr, sizeof(struct sockaddr_un));
    addr.sun_family = AF_UNIX;

    const char *nativePath = (*jenv)->GetStringUTFChars(jenv, jpath, 0);
    strcpy(addr.sun_path, nativePath);
    (*jenv)->ReleaseStringUTFChars(jenv, jpath, nativePath);

    if (bind(jsocket, (struct sockaddr_t *) &addr, sizeof(addr)) < 0) {
        throwIOException(jenv, "error in bind");
        return;
    }
}

//     public static native void connect(int socket, String path) throws UnixSocketException;
JNIEXPORT void JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_connect(JNIEnv *jenv, jclass jClass, jint jsocket, jstring jpath) {
    struct sockaddr_un addr;
    bzero(&addr, sizeof(struct sockaddr_un));
    addr.sun_family = AF_UNIX;

    const char *nativePath = (*jenv)->GetStringUTFChars(jenv, jpath, 0);
    strcpy(addr.sun_path, nativePath);
    (*jenv)->ReleaseStringUTFChars(jenv, jpath, nativePath);

    if (connect(jsocket, (struct sockaddr_t *) &addr, sizeof(addr)) < 0) {
        throwIOException(jenv, "error in bind");
        return;
    }
}

//     public static native void listen(int socket, int backlog);
JNIEXPORT void JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_listen(JNIEnv *jenv, jclass jClass, jint jsocket, jint jbacklog) {
    if (listen(jsocket, jbacklog) < 0) {
        throwIOException(jenv, "error in listen");
        return;
    }
}



//    public static native int accept(int socket, long timeoutMs) throws UnixSocketException;
// returns 0 in case that the accept timed out
JNIEXPORT jint JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_accept(JNIEnv *jenv, jclass jClass, jint jsocket, jlong timeoutMs) {
    struct sockaddr_un addr;
    bzero(&addr, sizeof(struct sockaddr_un));
    socklen_t rlen;

    fd_set set;
    struct timeval timeout;
    int rv;

    timeout.tv_sec = timeoutMs / 1000;
    timeout.tv_usec = (int) (timeoutMs % 1000) * 1000;

    FD_ZERO(&set); /* clear the set */
    FD_SET(jsocket, &set); /* add our file descriptor to the set */

    rv = select(jsocket + 1, &set, NULL, NULL, &timeout);
    if (rv == -1) {
        throwIOException(jenv, "error in select");
        return -1;
    } else if (rv == 0) {
        return 0;
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
    if (offset < 0 || length < 0) {
        throwIllegalArgumentException(jenv, "Invalid offset,  length or timeout");
        return -1;
    }


    jbyte *buf = (*jenv)->GetByteArrayElements(jenv, jbuf, NULL);
    if (jbuf == NULL) {
        return -1;
    }


    jint bufLen = (*jenv)->GetArrayLength(jenv, jbuf);
    if (length > (bufLen - offset)) {
        length = bufLen - offset;
    }


    // TODO honor timeout here
    ssize_t rcount = read(jsocket, &(buf[offset]), (size_t) length);
    (*jenv)->ReleaseByteArrayElements(jenv, jbuf, buf, 0);


    if (rcount == 0) {
        return -1;
    } else if (rcount < 0) {
        throwIOException(jenv, "Error reading from socket");
        return -1;
    }
    return (jint) rcount;

}


//     public static native int send(int socket, byte[] buffer) throws UnixSocketException;
JNIEXPORT jint JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_send(JNIEnv *jenv, jclass jClass, jint jsocket, jbyteArray jbuf,
                                                        jint offset, jint length) {
    if (offset < 0 || length < 0) {
        throwIllegalArgumentException(jenv, "Invalid offset,  length or timeout");
        return -1;
    }


    jbyte *buf = (*jenv)->GetByteArrayElements(jenv, jbuf, NULL);
    if (jbuf == NULL) {
        return -1;
    }


    jint bufLen = (*jenv)->GetArrayLength(jenv, jbuf);
    if (length > (bufLen - offset)) {
        throwIllegalArgumentException(jenv, "Invalid length, beyond end of buffer");
        return -1;
    }


    ssize_t wcount = write(jsocket, &(buf[offset]), (size_t) length);
    (*jenv)->ReleaseByteArrayElements(jenv, jbuf, buf, 0);
    if (wcount < 0) {
        throwIOException(jenv, "Error reading from socket");
        return -1;
    }

    return (jint) wcount;

}




//     public static native  close(int socket);


JNIEXPORT void JNICALL
Java_com_fnproject_fn_runtime_ntv_UnixSocketNative_close(JNIEnv *jenv, jclass jClass, jint jsocket) {

    if (close(jsocket) < 0) {
        throwIOException(jenv, "Error in closing socket");
        return;
    }
}