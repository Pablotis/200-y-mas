package pablo.criado.albillos.doscientosymas;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    Socket socket;
    Thread socketThread, readThread = new Thread(new Runnable() {
        @Override
        public void run() {
            try {
                DataInputStream inputStream = new DataInputStream(socket.getInputStream());
                while (!Thread.interrupted()) {
                    switch (inputStream.read()) {
                        case 0:
                            inputStream.read(new byte[3], 0, 3);
                        case 1:
                            inputStream.read();
                            byte[] data = new byte[inputStream.readInt()];
                            inputStream.readFully(data, 0, data.length);
                            JSONObject object = new JSONObject(new String(data));
                            ((ListAdapter) recyclerView.getAdapter()).setJSONData(object.getJSONArray("data"));
                            break;
                    }
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                socketThread.interrupt();
            }
        }
    });

    private void connect() {
        socketThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket("pablocloud.es", 2000);
                    socketThread.start();
                    socket.getOutputStream().write(new byte[]{1, 2});
                } catch (IOException e) {
                    e.printStackTrace();
                    readThread.interrupt();
                }
            }
        });
        socketThread.start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new ListAdapter(this));

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        connect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        //int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /*if (id == R.id.action_settings) {
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }
}
