package pablo.criado.albillos.doscientosymas;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TextInputEditText;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
    RecyclerView recyclerView;
    Socket socket;
    SwipeRefreshLayout swipeRefreshLayout;
    Thread socketThread, readThread, updateDataThread, sendValorThread, setLikeThread;
    ProgressDialog sendingDialog;
    boolean showBest;

    private void connect() {
        if (socketThread != null) socketThread.interrupt();
        socketThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (socket == null || socket.isClosed()) {
                        socket = new Socket();
                        socket.connect(new InetSocketAddress("pablocloud.es", 2000), 2000);
                    }
                    if (readThread != null) readThread.interrupt();
                    readThread = new Thread(new Runnable() {
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
                                            final byte[] data = new byte[inputStream.readInt()];
                                            inputStream.readFully(data, 0, data.length);
                                            runOnUiThread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    try {
                                                        JSONObject object = new JSONObject(new String(data));
                                                        ((ListAdapter) recyclerView.getAdapter()).setJSONData(object.getJSONArray("data"));
                                                    } catch (JSONException e) {
                                                        e.printStackTrace();
                                                    }
                                                    swipeRefreshLayout.setRefreshing(false);
                                                }
                                            });
                                            break;
                                        case 2:
                                            //socket.close();
                                            Thread.currentThread().interrupt();
                                            break;
                                        case 3:
                                            inputStream.read();
                                            sendingDialog.dismiss();
                                            updateList();
                                            break;
                                    }
                                }
                                socket.close();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        swipeRefreshLayout.setRefreshing(false);
                                    }
                                });
                            } catch (IOException e) {
                                e.printStackTrace();
                                showError();
                                try {
                                    socket.close();
                                } catch (IOException e2) {
                                    e2.printStackTrace();
                                }
                            }
                        }
                    });
                    readThread.start();
                    updateList();
                } catch (IOException e) {
                    e.printStackTrace();
                    showError();
                    try {
                        socket.close();
                    } catch (IOException e2) {
                        e2.printStackTrace();
                    }
                    //readThread.interrupt();
                }
            }
        });
        socketThread.start();
    }

    private void disconnect() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (socket != null && socket.isConnected()) {
                        socket.getOutputStream().write(new byte[]{2, 0});
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void updateList() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(true);
            }
        });
        if (socket == null || socket.isClosed()) connect();
        else {
            updateDataThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ByteBuffer buffer = ByteBuffer.allocate(103);
                        buffer.put(new byte[]{1, 0, showBest ? (byte) 1 : 0});
                        buffer.put(PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("id", "").getBytes());
                        socket.getOutputStream().write(buffer.array());
                    } catch (IOException e) {
                        e.printStackTrace();
                        showError();
                    }
                }
            });
            updateDataThread.start();
        }
    }

    private void sendValor(final String name, final String valor) {
        if (socket == null || socket.isClosed()) connect();
        else {
            sendingDialog = new ProgressDialog(this);
            sendingDialog.setIndeterminate(true);
            sendingDialog.setMessage(getString(R.string.sending));
            //sendingDialog.show();
            sendValorThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        JSONObject data = new JSONObject();
                        data.put("name", name);
                        data.put("valor", valor);
                        byte[] dataString = data.toString().getBytes();
                        ByteBuffer buffer = ByteBuffer.allocate(106 + dataString.length);
                        buffer.put(new byte[]{3, 0});
                        buffer.put(PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("id", "").getBytes());
                        buffer.putInt(dataString.length);
                        buffer.put(dataString);
                        socket.getOutputStream().write(buffer.array());
                    } catch (IOException | JSONException e) {
                        e.printStackTrace();
                        showError();
                    }
                }
            });
            sendValorThread.start();
        }
    }

    public void setLike(final String id, final boolean like) {
        if (socket == null || socket.isClosed()) connect();
        else {
            setLikeThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ByteBuffer buffer = ByteBuffer.allocate(127);
                        buffer.put(new byte[]{4, 0, like ? (byte) 1 : 0});
                        buffer.put(PreferenceManager.getDefaultSharedPreferences(MainActivity.this).getString("id", "").getBytes());
                        buffer.put(id.getBytes());
                        socket.getOutputStream().write(buffer.array());
                    } catch (IOException e) {
                        e.printStackTrace();
                        showError();
                    }
                }
            });
            setLikeThread.start();
        }
    }

    private void showError() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                swipeRefreshLayout.setRefreshing(false);
                ((ListAdapter) recyclerView.getAdapter()).setNoConnection();
            }
        });
    }

    @Override
    protected void onDestroy() {
        disconnect();
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (PreferenceManager.getDefaultSharedPreferences(this).getString("id", null) == null) {
            char[] chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
            StringBuilder sb = new StringBuilder();
            Random random = new Random();
            for (int i = 0; i < 100; i++) {
                sb.append(chars[random.nextInt(chars.length)]);
            }
            PreferenceManager.getDefaultSharedPreferences(this).edit().putString("id", sb.toString()).apply();
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new ListAdapter(this, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateList();
            }
        }));

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                updateList();
            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                        .setView(R.layout.add_item_dialog)
                        .setPositiveButton(getString(R.string.submit), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                TextInputEditText publicNameText = (TextInputEditText) ((AlertDialog) dialog).findViewById(R.id.publicNameText);
                                TextInputEditText valorText = (TextInputEditText) ((AlertDialog) dialog).findViewById(R.id.valorText);
                                sendValor(publicNameText.getText().toString(), valorText.getText().toString());
                            }
                        })
                        .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setCancelable(true)
                        .setTitle(R.string.new_valor)
                        .create();
                dialog.show();
                final Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
                final TextInputEditText publicNameText = (TextInputEditText) dialog.findViewById(R.id.publicNameText);
                final TextInputEditText valorText = (TextInputEditText) dialog.findViewById(R.id.valorText);
                positiveButton.setEnabled(false);
                TextWatcher textWatcher = new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                    }

                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        if (publicNameText.getText().length() > 0 && valorText.getText().length() > 0 && valorText.getText().length() <= 140)
                            positiveButton.setEnabled(true);
                        else positiveButton.setEnabled(false);
                    }

                    @Override
                    public void afterTextChanged(Editable s) {

                    }
                };
                publicNameText.addTextChangedListener(textWatcher);
                valorText.addTextChangedListener(textWatcher);
            }
        });

        updateList();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        menu.getItem(0).getIcon().setColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_ATOP);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_change_order) {
            showBest = !showBest;
            updateList();
            item.setTitle(showBest ? R.string.show_latest : R.string.show_best);
            item.setIcon(showBest ? R.drawable.ic_new_releases_black_24dp : R.drawable.ic_trending_up_black_24dp);
            item.getIcon().setColorFilter(0xFFFFFFFF, PorterDuff.Mode.SRC_ATOP);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
