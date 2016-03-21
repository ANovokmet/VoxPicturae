//
// Created by Ante on 26.2.2016..
//
#include <jni.h>
#include <stdio.h>
#include <android/bitmap.h>
#include <cstring>
#include <unistd.h>
#include <stdexcept>

extern "C"
{
jobject Java_hr_image_BitmapImageProcessingThread_loadBitmap(JNIEnv * env, jobject obj, jobject bitmap);
jboolean Java_hr_probavoice_CameraActivity_loadMask(JNIEnv * env, jobject obj, jintArray maskpixels, jint width, jint height);
jboolean Java_hr_image_BitmapImageProcessingThread_loadMask(JNIEnv * env, jobject obj, jintArray maskpixels, jint width, jint height);
jobject Java_hr_image_BitmapImageProcessingThread_loadArray(JNIEnv * env, jobject obj, jobject bitmap);
jboolean Java_hr_image_BitmapImageProcessingThread_convertYUV420_1NV21toRGB8888(JNIEnv * env, jobject obj, jbyteArray N21FrameData, jint width, jint height, jintArray outpixels, jintArray outhelppixels);
}
/*
 * Primjer korištenja android/bitmap.h
 * možda bude korisno, ako uvidim da bez bitmapa ne možemo i ako stvaranje bitmapa u native kodu bude brže
 */
jobject Java_hr_image_BitmapImageProcessingThread_loadBitmap(JNIEnv * env, jobject obj, jobject bitmap)
{
    //
    //getting bitmap info:
    //
    AndroidBitmapInfo info;
    int ret;
    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0)
    {
        return NULL;
    }
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888)
    {
        return NULL;
    }
    //
    //read pixels of bitmap into native memory :
    //
    void* bitmapPixels;
    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &bitmapPixels)) < 0)
    {
        return NULL;
    }
    uint32_t* src = (uint32_t*) bitmapPixels;
    uint32_t* tempPixels = new uint32_t[info.height * info.width];
    int stride = info.stride;
    int pixelsCount = info.height * info.width;
    memcpy(tempPixels, src, sizeof(uint32_t) * pixelsCount);
    AndroidBitmap_unlockPixels(env, bitmap);
    //
    //recycle bitmap - using bitmap.recycle()
    //
    jclass bitmapCls = env->GetObjectClass(bitmap);
    jmethodID recycleFunction = env->GetMethodID(bitmapCls, "recycle", "()V");
    if (recycleFunction == 0)
    {
        return NULL;
    }
    env->CallVoidMethod(bitmap, recycleFunction);
    //
    //creating a new bitmap to put the pixels into it - using Bitmap Bitmap.createBitmap (int width, int height, Bitmap.Config config) :
    //
    jmethodID createBitmapFunction = env->GetStaticMethodID(bitmapCls, "createBitmap", "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    jstring configName = env->NewStringUTF("ARGB_8888");
    jclass bitmapConfigClass = env->FindClass("android/graphics/Bitmap$Config");
    jmethodID valueOfBitmapConfigFunction = env->GetStaticMethodID(bitmapConfigClass, "valueOf", "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;");
    jobject bitmapConfig = env->CallStaticObjectMethod(bitmapConfigClass, valueOfBitmapConfigFunction, configName);
    jobject newBitmap = env->CallStaticObjectMethod(bitmapCls, createBitmapFunction, info.height, info.width, bitmapConfig);
    //
    // putting the pixels into the new bitmap:
    //
    if ((ret = AndroidBitmap_lockPixels(env, newBitmap, &bitmapPixels)) < 0)
    {
        return NULL;
    }
    uint32_t* newBitmapPixels = (uint32_t*) bitmapPixels;
    int whereToPut = 0;
    for (int x = info.width - 1; x >= 0; --x)
        for (int y = 0; y < info.height; ++y)
        {
            uint32_t pixel = tempPixels[info.width * y + x];
            newBitmapPixels[whereToPut++] = pixel;
        }
    AndroidBitmap_unlockPixels(env, newBitmap);
    //
    // freeing the native memory used to store the pixels
    //
    delete[] tempPixels;
    return newBitmap;
}

/*
 * Glupi testovi
 */
jbyteArray Java_hr_image_BitmapImageProcessingThread_loadArray(JNIEnv * env, jobject obj, jint width, jint height, jbyteArray NV21FrameData) {

    jbyte* pNV21FrameData = env->GetByteArrayElements(NV21FrameData, 0);

    jbyte* array;
    array=new jbyte[width * height];

    for (int x = 0; x < width; x++) {
        for (int y = 0; y < height; y++) {
        }
    }

    env->ReleaseByteArrayElements(NV21FrameData, pNV21FrameData, 0);

}

/*
 * Crveni kanal piksela (pixel & 0x000000ff)
 */
int red(int pixel){
    return pixel & 0x000000ff;
}
/*
 * Zeleni kanal piksela (pixel & 0x0000ff00)
 */
int green(int pixel){
    return (pixel & 0x0000ff00)>>8;
}
/*
 * Plavi kanal piksela (pixel & 0x00ff0000)
 */
int blue(int pixel){
    return (pixel & 0x00ff0000)>>16;
}

/*
 * Alfa kanal piksela (pixel & 0xff000000)
 */
int alpha(int pixel){
    return (pixel & 0xff000000)>>24;
}

/*
 * Stvaranje ARGB piksela pri èemu je alpha = 255 (neprozirno)
 * Svaka od vrijednosti mora biti u intervalu 0-255
 */
int rgbPixel(int r, int g, int b){
    return 0xff000000 | (b<<16) | (g<<8) | r;
}

/*
 * Stvaranje ARGB piksela
 * Svaka od vrijednosti mora biti u intervalu 0-255
 */
int rgbaPixel(int r, int g, int b, int a){
    return (r<<24) | (b<<16) | (g<<8) | r;
}

/*
 * Transformacija jbyteArray->unsigned char*(byte)
 * Ne koristi se jer je sporije
 */
unsigned char* as_unsigned_char_array(JNIEnv * env, jbyteArray array) {
    int len = env->GetArrayLength (array);
    unsigned char* buf = new unsigned char[len];
    env->GetByteArrayRegion (array, 0, len, reinterpret_cast<jbyte*>(buf));
    return buf;
}
/*
 * Transformacija unsigned char*(byte)->jbyteArray
 * Ne koristi se jer je sporije
 */
jbyteArray as_byte_array(JNIEnv * env, unsigned char* buf, int len) {
    jbyteArray array = env->NewByteArray (len);
    env->SetByteArrayRegion (array, 0, len, reinterpret_cast<jbyte*>(buf));
    return array;
}

/*
 * Koristi se pri transformaciji yuv->rgb
 * Koristi konstante s wikipedije
 */
int convertYUVtoRGB(int y, int u, int v) {
    int r,g,b;

    b = y + (int)1.370705f*v;//r = y + 1.402f*v;
    g = y - (int)(0.337633f*u +0.698001f*v);//g = y - (0.344f*u +0.714f*v);
    r = y + (int)2.032446f*u;//b = y + 1.772f*u; //koristi se novokmet-konstanta umjesto 1.732446
    r = r>255? 255 : r<0 ? 0 : r;
    g = g>255? 255 : g<0 ? 0 : g;
    b = b>255? 255 : b<0 ? 0 : b;
    return 0xff000000 | (b<<16) | (g<<8) | r;
}

/*
 * Odsijecanje vrijednosti
 * min, max predvidivo 0, 255
 */
int clamp(int value, int min, int max){
    if(value < min) return min;
    if(value > max) return max;
    return value;
}

/*
 * Probne funkcije za transformaciju kanala
 * Vrijednosti se mogu dobiti npr photoshopiranjem slike sa 16 sivih boja i oèitavanjem vrijednosti
 */
int function0[17] = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17 };//mozda bit ih 17
int functionr[17] = { 41, 48, 57, 65, 72, 79, 86, 91, 96, 104, 109, 115, 122, 132, 143, 154, 165 };//mozda bit ih 17
int functiong[17] = { 4, 10, 20, 34, 52, 73, 94, 117, 139, 157, 177, 192, 204, 214, 223, 229, 235 };
int functionb[17] = { 23, 41, 63, 87, 115, 139, 163, 185, 204, 219, 230, 238, 244, 247, 250, 252, 255 };

int transformChannelUsingFunction(int value, int* function){
    int segment = value >> 4;

    int seg0 = function[segment];
    int seg1 = function[segment+1];

    return seg0 + (seg1-seg0) * (value%16) / 16.0f;
}

/*
 * Normalizacija kanala je linearno preslikavanje
 * npr 0.5 s intervala 0-1, na interval 5-10 je 7.5
 * korisna funkcija
 */
int normalizeChannel(int value, int sourceMin, int sourceMax, int destMin, int destMax){
    value = clamp(value, sourceMin, sourceMax);

    int sourceRange = sourceMax - sourceMin;
    int destRange = destMax - destMin;

    float scale = (float)destRange / sourceRange;

    return destMin + (value - sourceMin) * scale;
}

/*
 * Sepija je èisto množenje kanala s fiksnim parametrima
 * Implementirani su preporuèeni od microsofta
 */
int sepia(int pixel){
    int r,g,b;

    r = red(pixel);
    g = green(pixel);
    b = blue(pixel);

    return rgbPixel(
            clamp((int)(r * 0.393f + g * 0.769f + b * 0.189f), 0, 255),
            clamp((int)(r * 0.349f + g * 0.686f + b * 0.168f), 0, 255),
            clamp((int)(r * 0.272f + g * 0.534f + b * 0.131f), 0, 255)
    );
}

/*
 * Kontrast kanala, radi na principu udaljavanja boje od neke srednišnje vrijednosti
 * c = 0, siva boja
 * c = 1, nema promjene
 */
int contrastChannel(float f, float c){
    return (((f / 255.0f - 0.5f) * c + 0.5f) * 255.0f);
}

/*
 * Saturacija kanala, radi na principu udaljavanja boje od neke srednišnje vrijednosti
 * c = 0, "crno-bijela" vrijednost kanala
 * c = 1, nema promjene
 */
int saturateChannel(float f, float average, float amount){
    return (f - average) * amount + average;
}

/*
 * Jednostavna manipulacija boje kanala
 * Dodaje postotak naznaèen amountom
 */
int adjustChannel(float f, float amount){
    return f * (1 + amount);
}

/*
 * Jednostavna manipulacija boje kanala
 * Dodaje fiksnu kolièinu naznaèenu amountom
 */
int brightenChannel(float f, float amount){
    return f + amount;
}

/*
 * Kontrast pojedinog piksela
 * Moguæe zamijeniti kontrastiranjem pojedinog kanala
 *
 */
int contrast(int pixel, float amount){ // amount€[0,2], 1 no effect
    int r,g,b;

    r = red(pixel);
    g = green(pixel);
    b = blue(pixel);

    return rgbPixel(
            clamp(contrastChannel((float)r, amount), 0, 255),
            clamp(contrastChannel((float)g, amount), 0, 255),
            clamp(contrastChannel((float)b, amount), 0, 255)
    );
}

/*
 * Saturacija pojedinog piksela
 * Potrebno je izraèunati average te stoga se ne može samo zamijeniti saturacijama kanala
 */
int saturate(int pixel, float amount){// amount€[0,2], 1 no effect
    int r,g,b;

    r = red(pixel);
    g = green(pixel);
    b = blue(pixel);

    float average = (float)(r + g + b) / 3;

    return rgbPixel(
            clamp(saturateChannel((float)r, average, amount), 0, 255),
            clamp(saturateChannel((float)g, average, amount), 0, 255),
            clamp(saturateChannel((float)b, average, amount), 0, 255)
    );
}


/*
 * test funkcija u kojoj se može zadati transformacija za piksel
 * brže je raditi transformaciju nad pojedinim kanalima unutar YUV->RGB tranformacije
 * možeš odkomentirati pojedine returnove za ono... neš fora
 * Konvolucije se rade unutar one fukcije koja prima input iz jave, yuv->rgb, nakon transformacije cijele slike
 */
int applyEffect(int pixel){

    //return pixel;

    int r,g,b;

    r = red(pixel);
    g = green(pixel);
    b = blue(pixel);

    return saturate(/*contrast(*/rgbPixel(
            normalizeChannel(r, 0, 255, 39, 248),
            normalizeChannel(g, 0, 255, 96, 213),
            normalizeChannel(b, 0, 255, 77, 162)
    )/*, 3.0f)*/, 2.0f);

    return rgbPixel(
            transformChannelUsingFunction(r, functionr),
            transformChannelUsingFunction(g, functiong),
            transformChannelUsingFunction(b, functionb)
    );
}

float kernel1[3] = {1.0f/3, 1.0f/3, 1.0f/3};
/*
 * Konvoluira matricu s kernelom (matrica) koji ima odreðeni radius (parapetar je dijametar)
 * Izvodi se u horizontalnom i vertikalnom smjeru s jednodimenzionalnom matricom èiji je svaki element 1/N (optimizacija)
 * N = duljina matrice, treba biti neparan
 * float* kernel nema utjecaj jer se ne zapravo ne koristi kernel veæ izraèunava 1/N
 * kernel bi trebao imati zbroj elemenata = 1
 */
void convolveMatrixWithKernel(int* pixels, int* help, float* kernel, int width, int height, int kernelWidth, int kernelHeight){

    for(int x = 0; x < width; x++){
        for(int y = 0; y < height; y++){
            int pixel = 0;
            int r = 0, g = 0, b = 0;

            for(int i = x-kernelWidth/2; i < x+kernelWidth/2+1; i++){
                if(i<0){
                    pixel = pixels[width*y];
                }
                else if(i>=width){
                    pixel = pixels[width*(y+1)-1];
                }
                else{
                    pixel = pixels[width*y+i];
                }

                r += red(pixel);
                g += green(pixel);
                b += blue(pixel);
            }

            r /= kernelWidth;
            g /= kernelWidth;
            b /= kernelWidth;

            help[width*y+x] = rgbPixel(r,g,b);
        }
    }

    for(int x = 0; x < width; x++){
        for(int y = 0; y < height; y++){
            int pixel = 0;
            int r = 0, g = 0, b = 0;

            for(int i = y-kernelHeight/2; i < y+kernelHeight/2+1; i++){
                if(i<0){
                    pixel = help[x];
                }
                else if(i>=height){
                    pixel = help[width*(height-1)+x];
                }
                else{
                    pixel = help[width*i+x];
                }

                r += red(pixel);
                g += green(pixel);
                b += blue(pixel);
            }

            r /= kernelHeight;
            g /= kernelHeight;
            b /= kernelHeight;

            pixels[width*y+x] = rgbPixel(r,g,b);
        }
    }

}


/*
 * Sa stackoverflowa (vjerojatno) naèin dobivanja iznimki natrag u javi
 * Probavao sam throwati iznimke namjerno i nije radilo
 */
void throwJavaException(JNIEnv *env, const char *msg)
{
    jclass c = env->FindClass("java/lang/RuntimeException");

    if (NULL == c)
    {
        c = env->FindClass("java/lang/NullPointerException");
    }

    env->ThrowNew(c, msg);
}

int* maskPixels;
int maskWidth;
int maskHeight;

/*
 * Konvoluira matricu s kernelom (matrica) koji ima promjenjiv radius
 * U izradi je
 * Izvodi se u horizontalnom i vertikalnom smjeru s jednodimenzionalnom matricom èiji je svaki element 1/N
 * N = duljina matrice, treba biti neparan
 * Radi u svakom smjeru posebno ali zajedno se app crasha
 * float* kernel nema utjecaj jer se ne zapravo ne koristi kernel veæ izraèunava 1/N
 * treba napraviti novu masku jer brijem da ova nema dovoljno izražene koeficijente
 */
void convolveMatrixWithKernelVariable(JNIEnv * env, int* pixels, int* help, float* kernel, int width, int height, int kernelWidth, int kernelHeight, int* maskPixels){

    if(maskPixels == NULL){
        return;
    }
/*
    float maxFactorX = 1 / (float)kernelWidth;


    for(int x = 0; x < width; x++){
        for(int y = 0; y < height; y++){
            int pixel = 0;
            int r = 0, g = 0, b = 0;

            int factor = red(maskPixels[width*y + x]);//any channel

            if(factor == 0) {
                continue;
            }

            float maskFactor = (float)factor/(float)255;

            float fn = maxFactorX * maskFactor;

            for(int i = x-kernelWidth/2; i < x+kernelWidth/2+1; i++){
                if(i<0){
                    pixel = pixels[width*y];
                }
                else if(i>=width){
                    pixel = pixels[width*(y+1)-1];
                }
                else{
                    pixel = pixels[width*y+i];
                }

                if(i == x){
                    //sredisnji (1-F)+F/N
                    float ffn = 1-maskFactor+fn;
                    r += red(pixel) * ffn;
                    g += green(pixel) * ffn;
                    b += blue(pixel) * ffn;
                }
                else{
                    //sporedni F/N
                    r += red(pixel) * fn;
                    g += green(pixel) * fn;
                    b += blue(pixel) * fn;
                }
            }

            r = clamp(r,0,255);
            g = clamp(g,0,255);
            b = clamp(b,0,255);

            help[width*y+x] = rgbPixel(r,g,b);
        }
    }*/

    /*for(int x = 0; x < width; x++){
        for(int y = 0; y < height; y++){
            int pixel = 0;
            int r = 0, g = 0, b = 0;

            int factor = red(maskPixels[width*y + x]);
            if(factor == 0) {
                continue;
            }
            float maskFactor = (float)factor/(float)255;
            int kernelW = (int)(kernelWidth * maskFactor);
            if(kernelW%2==0){
                kernelW++;
            }

            for(int i = x-kernelW/2; i < x+kernelW/2+1; i++){
                if(i<0){
                    pixel = pixels[width*y];
                }
                else if(i>=width){
                    pixel = pixels[width*(y+1)-1];
                }
                else{
                    pixel = pixels[width*y+i];
                }

                r += red(pixel);
                g += green(pixel);
                b += blue(pixel);
            }

            r /= kernelW;
            g /= kernelW;
            b /= kernelW;

            help[width*y+x] = rgbPixel(r,g,b);
        }
    }*/

    for(int x = 0; x < width; x++){
        for(int y = 0; y < height; y++){
            int pixel = 0;
            int r = 0, g = 0, b = 0;


            int factor = red(maskPixels[width*y + x]);
            /*if(factor == 0) {
                continue;
            }*/


            float maskFactor = (float)factor/(float)255;
            int kernelH = (int)(kernelHeight * maskFactor);
            if(kernelH%2==0){
                kernelH++;
            }

            for(int i = y-kernelH/2; i < y+kernelH/2+1; i++){
                if(i<0){
                    pixel = pixels[x];
                }
                else if(i>=height){
                    pixel = pixels[width*(height-1)+x];
                }
                else{
                    pixel = pixels[width*i+x];
                }

                r += red(pixel);
                g += green(pixel);
                b += blue(pixel);
            }

            r /= kernelH;
            g /= kernelH;
            b /= kernelH;

            help[width*y+x] = rgbPixel(r,g,b);
        }
    }

    for(int x = 0; x < width; x++){
        for(int y = 0; y < height; y++) {
            pixels[width*y+x] = help[width*y+x];
        }

    }

/*
    float maxFactorY = 1 / (float)kernelHeight;

    for(int x = 0; x < width; x++){
        for(int y = 0; y < height; y++){
            int pixel = 0;
            int r = 0, g = 0, b = 0;

            int factor = red(maskPixels[width*y + x]);//any channel
            float maskFactor = (float)factor/(float)255;

            if(maskFactor == 0){
                continue;
            }

            float fn = maxFactorY * maskFactor;

            for(int i = y-kernelHeight/2; i < y+kernelHeight/2+1; i++){
                if(i<0){
                    pixel = help[x];
                }
                else if(i>=height){
                    pixel = help[width*(height-1)+x];
                }
                else{
                    pixel = help[width*i+x];
                }

                if(i == y){
                    //sredisnji (1-F)+F/N
                    float ffn = 1-maskFactor+fn;
                    r += red(pixel) * ffn;
                    g += green(pixel) * ffn;
                    b += blue(pixel) * ffn;
                }
                else{
                    //sporedni F/N
                    r += red(pixel) * fn;
                    g += green(pixel) * fn;
                    b += blue(pixel) * fn;
                }
            }

            r = clamp(r,0,255);
            g = clamp(g,0,255);
            b = clamp(b,0,255);

            pixels[width*y+x] = rgbPixel(r,g,b);
        }
    }*/
}

/**
 * Converts YUV420 NV21 to RGB8888
 *
 * @param data byte array on YUV420 NV21 format.
 * @param width pixels width
 * @param height pixels height
 * @return a RGB8888 pixels int array. Where each int is a pixels ARGB.
 *
 * N21Framedata je podatak o preview frameu
 * outpixels je izlazni int[], sadrži piksele
 * outhelppixels se koristi pri blurranju koji radi u 2 smjera te je potrebna meðuvarijabla, moguæe izbaciti
 */
jboolean Java_hr_image_BitmapImageProcessingThread_convertYUV420_1NV21toRGB8888(JNIEnv * env, jobject obj, jbyteArray N21FrameData, jint jwidth, jint jheight, jintArray outpixels, jintArray outhelppixels) {
    try {
        int width = (int) jwidth;
        int height = (int) jheight;

        int size = width * height;
        int offset = size;
        //int *pixels = new int[size];
        int u, v, y1, y2, y3, y4;

        //unsigned char *data = as_unsigned_char_array(env, N21FrameData);

        jbyte* data = env->GetByteArrayElements(N21FrameData, 0);
        jint* pixels = env->GetIntArrayElements(outpixels, 0);
        jint* helppixels = env->GetIntArrayElements(outpixels, 0);

        int j = height - 1;
        int cj = j;

        for (int i = 0, k = 0; i < size; i += 2, k += 2) {
            y1 = data[i] & 0xff;
            y2 = data[i + 1] & 0xff;
            y3 = data[width + i] & 0xff;
            y4 = data[width + i + 1] & 0xff;

            u = data[offset + k] & 0xff;
            v = data[offset + k + 1] & 0xff;
            u = u - 128;
            v = v - 128;

            //unrotated - orginalni kod
            /*pixels[i] = applyEffect(convertYUVtoRGB(y1, u, v));
            pixels[i + 1] = applyEffect(convertYUVtoRGB(y2, u, v));
            pixels[width + i] = applyEffect(convertYUVtoRGB(y3, u, v));
            pixels[width + i + 1] = applyEffect(convertYUVtoRGB(y4, u, v));*/

            //rotated - kompenzacija za zaokretanje slike prednje kamere na androidu
            pixels[j] = applyEffect(convertYUVtoRGB(y1, u, v));
            pixels[j + height] = applyEffect(convertYUVtoRGB(y2, u, v));
            pixels[j - 1] = applyEffect(convertYUVtoRGB(y3, u, v));
            pixels[j - 1 + height] = applyEffect(convertYUVtoRGB(y4, u, v));


            if (i != 0 && (i + 2) % width == 0) {
                i += width;
                cj -= 2;
                j = cj;
            }
            else
            j += 2 * height;
        }

        //Izvoðenje blurra
        //convolveMatrixWithKernel(pixels, helppixels, kernel1, height, width, 7, 7);
        //convolveMatrixWithKernelVariable(env , pixels, helppixels, kernel1, height, width, 21, 21, maskPixels);

        env->ReleaseByteArrayElements(N21FrameData, data, 0);
        env->ReleaseIntArrayElements(outpixels, pixels, 0);
        env->ReleaseIntArrayElements(outhelppixels, helppixels, 0);

        return true;
    }
    catch (std::exception e)
    {
        throwJavaException (env, e.what());
        return 0;
    }
}

/*
 * Uèitavanje maske preko druge klase
 * Ne funkcionira, array ostane neinicijaliziran
 */
jboolean Java_hr_probavoice_CameraActivity_loadMask(JNIEnv * env, jobject obj, jintArray pixels, jint width, jint height){
    maskWidth = width;
    maskHeight = height;
    maskPixels = env->GetIntArrayElements(pixels, 0);
    env->ReleaseIntArrayElements(pixels, maskPixels, 0);

    for(int x=0; x<maskWidth; x++){
        for(int y=0; y<maskHeight; y++){
            int factor = red(maskPixels[width*y+x]);
            int d = factor/255.0f;
        }
    }


    return true;
}

/*
 * Uèitavanje maske preko image klase
 * funkcionira
 */
jboolean Java_hr_image_BitmapImageProcessingThread_loadMask(JNIEnv * env, jobject obj, jintArray pixels, jint width, jint height){
    maskWidth = width;
    maskHeight = height;
    maskPixels = env->GetIntArrayElements(pixels, 0);
    env->ReleaseIntArrayElements(pixels, maskPixels, 0);

    for(int x=0; x<maskWidth; x++){
        for(int y=0; y<maskHeight; y++){
            int factor = red(maskPixels[width*y+x]);
            int d = factor/255.0f;
        }
    }
    return true;
}
