package com.pdm.musicon;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuItemCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.jean.jcplayer.model.JcAudio;
import com.example.jean.jcplayer.view.JcPlayerView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    String userName;

    private boolean checkPermission = false;
    Uri uri;
    String songName, songUrl;
    ListView listView;

    ArrayList<String> arrayListSongsName = new ArrayList<>();
    ArrayList<String> arrayListSongsUrl = new ArrayList<>();
    ArrayAdapter<String> arrayAdapter;

    JcPlayerView jcPlayerView ;
    ArrayList<JcAudio> jcAudios = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /**
         * Recibe el intent del StartActivity
         */
        Intent intent = getIntent();
        userName =
                intent.getStringExtra(StartActivity.EXTRA_MESSAGE);
        TextView textView = (TextView) findViewById(R.id.text_message);
        textView.setText("Welcome "+userName);
        /**
         * Inicia listiew y JsPlayer
         */
        listView = findViewById(R.id.myListView);
        jcPlayerView = findViewById(R.id.jcplayer);
        /**
         * Metodo para recuperar canciones
         */
        recoverSongs();
        /**
         * CLickListener para cada cancion del listView
         */
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                jcPlayerView.playAudio(jcAudios.get(i));
                jcPlayerView.setVisibility(View.VISIBLE);
                jcPlayerView.createNotification();
            }
        });

        if(jcPlayerView.isPlaying()){

            jcPlayerView.setVisibility(View.VISIBLE);
            jcPlayerView.createNotification();
        }
    }

    /**
     * Metodo para recuperar los archivos de firebase
     */
    private void recoverSongs() {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference("Songs");
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                /**
                 * Reinicia arrays
                 */
                arrayListSongsName.clear();
                arrayListSongsUrl.clear();
                jcAudios.clear();
                /**
                 * Recorre la BBDD
                 */
                for(DataSnapshot ds : dataSnapshot.getChildren()){
                    Song songObj = ds.getValue(Song.class);
                    arrayListSongsName.add(songObj.getSongName());
                    arrayListSongsUrl.add(songObj.getSongUrl());
                    jcAudios.add(JcAudio.createFromURL(songObj.getSongName(), songObj.getSongUrl()));
                }
                /**
                 * ArrayAdapter para el ViewList
                 */
                arrayAdapter = new ArrayAdapter<String>(MainActivity.this
                        ,android.R.layout.simple_list_item_1, arrayListSongsName){
                    @NonNull
                    @Override
                    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                        View view = super.getView(position, convertView,parent);
                        TextView textView = (TextView)view.findViewById(android.R.id.text1);
                        textView.setSingleLine(true);
                        textView.setMaxLines(1);
                        return view;
                    }
                };
                jcPlayerView.initPlaylist(jcAudios,null);
                listView.setAdapter(arrayAdapter);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    /**
     * Genera el menu
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.custom_menu, menu);

        return super.onCreateOptionsMenu(menu);

    }

    /**
     *  Al seleccionar el boton cargar del menu
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId()==R.id.nav_upload){

            if (validatePermision()){
                pickSong();
            }

        } else if (item.getItemId()==R.id.nav_search){
            Toast.makeText(getApplicationContext(), "Search pressed", Toast.LENGTH_SHORT).show();
        } else if (item.getItemId()==R.id.nav_info) {
            Toast.makeText(getApplicationContext(), "Made by Carlos Moreno", Toast.LENGTH_SHORT).show();
        }

        return super.onOptionsItemSelected(item);
    }

    private void search () {

    }

    /**
     * Metodo para iniciar otra activity en función de un resultado
     */
    private void pickSong() {

        Intent intent_upload = new Intent();
        /**
         * Selecciona el tipo de archivo
         */
        intent_upload.setType("audio/*");
        /**
         * Devuelve el URI del archivo
         */
        intent_upload.setAction(Intent.ACTION_GET_CONTENT);
        /**
         * Inicia una activity con codigo 1
         */
        startActivityForResult(intent_upload, 1);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        /**
         * Checkea el codigo del intent
         */
        if (requestCode == 1){
            if ( resultCode == RESULT_OK){

                /**
                 * Recibe el uri del intent
                 */
                uri  = data.getData();


                /**
                 * Recoge el nombre del archivo
                 */
                Cursor cursor = getApplicationContext().getContentResolver()
                        .query(uri,null, null,null, null);

                int indexedName = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                cursor.moveToFirst();
                songName = cursor.getString(indexedName);
                cursor.close();

                /**
                 * Llama al metodo para subir archivo a firebase
                 */
                uploadSongToFirebase();

            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Metodo para subir canciones a firebase
     */
    private void uploadSongToFirebase() {

        /**
         * Referencia a la base de datos
         */
        StorageReference storageReference = FirebaseStorage.getInstance().getReference()
                .child("Songs").child(uri.getLastPathSegment());

        /**
         * Muestra progeso de subida
         */
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.show();

        /**
         * Añanade archivo a la coleccion
         * El metodo putFile sube archivos desde un archivo local
         */
        storageReference.putFile(uri)
                /**
                 * Si se sube con exito
                 * @param taskSnapshot
                 */
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                while (!uriTask.isComplete());
                Uri urlSong = uriTask.getResult();
                songUrl = urlSong.toString();
                /**
                 * Llama al método para subir detalles del archivo
                 */
                uploadDetailsToDB();
                /**
                 * Una vez subida cierra mensaje de progreso
                 */
                progressDialog.dismiss();
            }
                /**
                 * Si hay algun fallo
                 */
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this,e.getMessage().toString(),
                        Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
                /**
                 * Muestra el progreso de subida
                 */
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                double progress = (100.0+taskSnapshot.getBytesTransferred()/taskSnapshot
                        .getTotalByteCount());
                int currentProgress = (int) progress;
                progressDialog.setMessage("Uploaded: "+currentProgress+"%");
            }
        });

    }

    /**
     * Termina subir los detalles a la BBDD
     */
    private void uploadDetailsToDB(){
        Song song = new Song(songName, songUrl);
        FirebaseDatabase.getInstance().getReference("Songs")
                .push().setValue(song).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    Toast.makeText(MainActivity.this, "Song Uploaded", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(MainActivity.this, e.getMessage().toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Metodo para validar permisos de acceso a la galeria del dispositivo
     * @return
     */
    private boolean validatePermision (){
        Dexter.withActivity(MainActivity.this)
                .withPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {
                        checkPermission = true;
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        checkPermission = false;
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
        return checkPermission;
    }
}
