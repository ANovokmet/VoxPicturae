//
// Created by Goran on 26.2.2016..
//

#include <string.h>
#include <jni.h>
//https://docs.oracle.com/javase/8/docs/technotes/guides/jni/spec/functions.html env funkcije
/*
 * dodati u local.properties ndk.dir= vrijednost
 * dodati u build.gradle od appa ndk { moduleName="imendkmodula" }
 * u java klasu native reprezentacije metoda i
 * static {
 *       System.loadLibrary("imendkmodula");
 *   }
 */

int Java_hr_probavoice_MainActivity_sumaArray(JNIEnv* env, jobject thiz, jintArray arej)
{
    int len = (*env)->GetArrayLength(env, arej);
    int i, sum=0;
    int* lokalni_array = (*env)->GetIntArrayElements(env, arej, NULL);
    for(i=0; i<len; i++){
        sum += lokalni_array[i];
    }
    (*env)->ReleaseIntArrayElements(env, arej, lokalni_array, JNI_ABORT);
    return sum;
}

jstring Java_hr_probavoice_MainActivity_dohvatiString(JNIEnv* env, jobject thiz)
{
    char* niz = "ajde";
    return (*env)->NewStringUTF(env, niz);
}
