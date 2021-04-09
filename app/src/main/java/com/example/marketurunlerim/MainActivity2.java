package com.example.marketurunlerim;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MainActivity2 extends AppCompatActivity {

    Bitmap selectedImage;
    ImageView imageView;
    EditText productNameText, marketNameText, priceText;
    Button button;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        imageView = findViewById(R.id.imageView);
        productNameText = findViewById(R.id.productNameText);
        marketNameText = findViewById(R.id.marketNameText);
        priceText = findViewById(R.id.priceText);
        button = findViewById(R.id.save);

        database = this.openOrCreateDatabase("Products",MODE_PRIVATE,null);

        //ilk aktiviteden tuslamayi karsiliyoruz.
        Intent intent = getIntent();
        String info = intent.getStringExtra("info");

        //if kullanici yeni urun eklemek istediyse.
        if (info.matches("new")){
            productNameText.setText("");
            marketNameText.setText("");
            priceText.setText("");
            button.setVisibility(View.VISIBLE);

            //bitmap ile fotografimizi defoult hale getiriyoruz.
            Bitmap selectImage = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.fotograf_sec);
            imageView.setImageBitmap(selectImage);

        } else {
            int productId = intent.getIntExtra("productId",1);
            button.setVisibility(View.INVISIBLE);

            try {

                Cursor cursor = database.rawQuery("SELECT * FROM products WHERE id = ?",new String[] {String.valueOf(productId)});

                int productNameIx = cursor.getColumnIndex("productname");
                int marketNameIx = cursor.getColumnIndex("marketname");
                int priceIx = cursor.getColumnIndex("price");
                int imageIx = cursor.getColumnIndex("image");

                while (cursor.moveToNext()){

                    productNameText.setText(cursor.getString(productNameIx));
                    marketNameText.setText(cursor.getString(marketNameIx));
                    priceText.setText(cursor.getString(priceIx));

                    byte[] bytes = cursor.getBlob(imageIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    imageView.setImageBitmap(bitmap);

                }

                cursor.close();

            } catch (Exception e){
                
            }
        }
    }

    public void selectImage (View view) {

        //asagida gerekli izni kontrol ettik. if faha once gerceklesmediyse izin istedik.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        } else {
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intentToGallery,2);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intentToGallery = new Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentToGallery,2);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == 2 && resultCode == RESULT_OK && data != null) {

            Uri imageData = data.getData();

            try {
                if (Build.VERSION.SDK_INT >= 28) {
                    ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(),imageData);
                    selectedImage = ImageDecoder.decodeBitmap(source);
                    imageView.setImageBitmap(selectedImage);
                } else {
                    selectedImage = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageData);
                    imageView.setImageBitmap(selectedImage);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void save (View view) {

        String productName = productNameText.getText().toString();
        String marketName = marketNameText.getText().toString();
        String value = priceText.getText().toString();

        Bitmap smallImage = makeSmallerImage(selectedImage,300);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray = outputStream.toByteArray();

        try {

            database = this.openOrCreateDatabase("Products",MODE_PRIVATE,null);
            database.execSQL("CREATE TABLE IF NOT EXISTS products (id INTEGER PRIMARY KEY,productname VARCHAR,marketname VARCHAR, price VARCHAR,image BLOB)");

            String sqlString = "INSERT INTO products (productname, marketname, price, image) VALUES (?,?,?,?)";
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
            sqLiteStatement.bindString(1,productName);
            sqLiteStatement.bindString(2,marketName);
            sqLiteStatement.bindString(3,value);
            sqLiteStatement.bindBlob(4,byteArray);
            sqLiteStatement.execute();

        } catch (Exception e) {

        }

        Intent intent = new Intent(MainActivity2.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);

        //finish ile aktiviteyi bitirmemiz gerekiyordu fakat boyle bitirdigimiz icin diger aktivitenin onClick metodu calismiyor.
        // bu yüzden listeye urun gelmiyordu. yani eski son liste gosteriliyordu, ytani guncel degildi. onClick e gitmesi lazim. cozumu(flag) ustte.
//        finish();

    }

    public Bitmap makeSmallerImage(Bitmap image, int maximumSize) {

        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height ;

        if (bitmapRatio > 1) {
            width = maximumSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maximumSize;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(image,width,height,true);
    }
}