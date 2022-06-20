package com.masterm1nd.fowler;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.view.MotionEvent;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OyunEkrani extends SurfaceView implements Runnable {
    private Thread thread;
    private boolean isPlaying, oyunBitti = false;
    private int ekranX, ekranY, skor = 0;
    public static float ekranBuyuklukX, ekranBuyuklukY;
    private Arkaplan arkaplan1,arkaplan2;
    private Ucak ucak;
    private Paint paint;
    private Kus[] kuslar;
    private SoundPool soundPool;
    private int sound;
    private SharedPreferences prefs;
    private Random rastgele;
    private List<Kursun> kursunlar;
    private OyunActivity activity;


    public OyunEkrani(OyunActivity activity, int ekranX, int ekranY) {
        super(activity);

        this.activity = activity;

        prefs = activity.getSharedPreferences("oyun", Context.MODE_PRIVATE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            AudioAttributes audioAttributes = new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .build();
            soundPool = new SoundPool.Builder().setAudioAttributes(audioAttributes).build();
        } else
            soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);

        sound = soundPool.load(activity, R.raw.ates_sesi, 1);

        this.ekranX = ekranX;
        this.ekranY = ekranY;
        ekranBuyuklukX = 1920f / ekranX;
        ekranBuyuklukY = 1080f / ekranY;

        arkaplan1 = new Arkaplan(ekranX, ekranY, getResources());
        arkaplan2 = new Arkaplan(ekranX, ekranY, getResources());

        ucak = new Ucak(this, ekranY, getResources());

        kursunlar = new ArrayList<>();

        arkaplan2.x=ekranX;

        paint = new Paint();
        paint.setTextSize(128);
        paint.setColor(Color.WHITE);

        kuslar = new Kus[4];

        for(int i=0; i < 4; i++){
            Kus kus = new Kus(getResources());
            kuslar[i] = kus;
        }

        rastgele = new Random();
    }

    @Override
    public void run() {
        while(isPlaying) {
            update();
            draw();
            sleep();
        }

    }
    private void update(){
        arkaplan1.x -= 10 * ekranBuyuklukX;
        arkaplan2.x -= 10 * ekranBuyuklukX;

        if (arkaplan1.x + arkaplan1.arkaplan.getWidth() < 0){
            arkaplan1.x = ekranX;
        }
        if (arkaplan2.x + arkaplan2.arkaplan.getWidth() < 0){
            arkaplan2.x = ekranX;
        }

        //Dokundukça, uçağı Y ekseninde yukarı ve aşağıya hareket ettirmek için
        if(ucak.yukariGit)
            ucak.y -= 30 * ekranBuyuklukY;
        else
            ucak.y += 30 * ekranBuyuklukY;
        //tıklamayı bıraktıgımız zaman  sıfır konumuna geri dön(ekranın sol en altına)
        if(ucak.y < 0)
            ucak.y = 0;
        //ekranın yarısının sol tarafında üste tıklandıgında ucak yükselsin
        if(ucak.y >= ekranY - ucak.yukseklik)
            ucak.y = ekranY - ucak.yukseklik;

        List<Kursun> bullet = new ArrayList<>();

        for(Kursun kursun : kursunlar){
            //ucaktan kursun cıktıysa kursunlar listesine ekle
            if(kursun.x > ekranX)
                bullet.add(kursun);
            kursun.x += 50 * ekranBuyuklukX;

            //kursunla kusun carpısması
            for(Kus kus : kuslar){
                //kursun kusa carptı
                if(Rect.intersects(kus.getCarpismaKontrol(), kursun.getCarpismaKontrol())){

                    skor++;
                    kus.x = -500;
                    kursun.x = ekranX + 500;
                    kus.vurulduKus = true;
                }
            }
        }
        for(Kursun kursun : bullet){
            kursunlar.remove(kursun);
        }

        for(Kus kus : kuslar){
            kus.x -= kus.hiz;

            if(kus.x + kus.genislik < 0){

                if(!kus.vurulduKus){
                    oyunBitti = true;
                    return;

                }

                int randomRelated = (int) (30 * ekranBuyuklukX);
                kus.hiz = rastgele.nextInt(randomRelated);

                if(kus.hiz < 10 * ekranBuyuklukX){
                    kus.hiz = (int) (10 * ekranBuyuklukX);
                }

                //kusların ekranın dısından bir yerden gelmemesi için
                kus.x = ekranX;
                kus.y = rastgele.nextInt(ekranY - kus.yukseklik);

                kus.vurulduKus = false;
            }

            if(Rect.intersects(kus.getCarpismaKontrol(), ucak.getCarpismaKontrol())){
                oyunBitti = true;
                return;
            }
        }


    }

    private void draw(){

        if(getHolder().getSurface().isValid()){

            Canvas canvas = getHolder().lockCanvas();
            canvas.drawBitmap(arkaplan1.arkaplan, arkaplan1.x, arkaplan1.y, paint);
            canvas.drawBitmap(arkaplan2.arkaplan, arkaplan2.x, arkaplan2.y, paint);

            for(Kus kus : kuslar){
                canvas.drawBitmap(kus.getKus(), kus.x, kus.y, paint);
            }

            canvas.drawText(skor + "", ekranX / 2f, 164, paint);

            if(oyunBitti){
                isPlaying = false;
                canvas.drawBitmap(ucak.getKaza(), ucak.x, ucak.y, paint);
                //kaza oldugunda arkaplan akışını durdur
                getHolder().unlockCanvasAndPost(canvas);
                //en yuksek skor metodu
                kaydetHighScore();
                //oyun bittikten sonra 2sn goruntuyu tut, sonra MainActivity'e geç
                bekleSonraGec();
                return;
            }

            canvas.drawBitmap(ucak.getUcak(), ucak.x, ucak.y, paint);

            for(Kursun kursun : kursunlar){
                canvas.drawBitmap(kursun.kursun, kursun.x, kursun.y, paint);
            }

            getHolder().unlockCanvasAndPost(canvas);

        }

    }

    private void bekleSonraGec() {

        try {
            //2sn bekle
            Thread.sleep(2000);
            //Main Activity'e git.
            activity.startActivity(new Intent(activity, MainActivity.class));
            activity.finish();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void kaydetHighScore() {

        if(prefs.getInt("yuksekskor", 0) < skor){
            SharedPreferences.Editor editor = prefs.edit();
            editor.putInt("yuksekskor", skor);
            editor.apply();
        }
    }

    private void sleep(){
        try {
            Thread.sleep(17);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void resume(){
        isPlaying=true;
        thread = new Thread(this);
        thread.start();


    }

    public void pause(){
        try {
            isPlaying=false;
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                if(event.getX() < ekranX / 2){
                    ucak.yukariGit = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                ucak.yukariGit = false;
                //ekranın sağ tarafına dokunulduğunda
                if(event.getX() > ekranX / 2)
                    ucak.atesEt++;
                break;
        }

        return true;
    }

    public void Kursun() {

        if(!prefs.getBoolean("sessiz", false))
            soundPool.play(sound, 1, 1, 0, 0,1);

        Kursun kursun = new Kursun(getResources());
        kursun.x = ucak.x + ucak.genislik;
        kursun.y = ucak.y + (ucak.yukseklik/2);
        kursunlar.add(kursun);

    }
}
