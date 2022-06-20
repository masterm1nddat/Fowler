package com.masterm1nd.fowler;

import static com.masterm1nd.fowler.OyunEkrani.ekranBuyuklukX;
import static com.masterm1nd.fowler.OyunEkrani.ekranBuyuklukY;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

public class Kursun {

    int x, y, genislik, yukseklik;
    Bitmap kursun;

    Kursun (Resources res){

        kursun = BitmapFactory.decodeResource(res, R.drawable.kursun);

        genislik = kursun.getWidth();
        yukseklik = kursun.getHeight();

        genislik /= 4;
        yukseklik /= 4;

        genislik = (int) (genislik * ekranBuyuklukX);
        yukseklik = (int) (yukseklik * ekranBuyuklukY);

        kursun = Bitmap.createScaledBitmap(kursun, genislik, yukseklik, false);
    }

    Rect getCarpismaKontrol(){
        return new Rect(x, y, x+genislik, y+yukseklik);
    }


}
