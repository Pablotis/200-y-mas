package pablo.criado.albillos.doscientosymas;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.VH> {
    private Context context;
    private ArrayList<ListItem> list = new ArrayList<>();

    public void setJSONData(JSONArray array) throws JSONException {
        ArrayList<ListItem> newList = new ArrayList<>();

        for (int a = 0; a < array.length(); a++) {
            newList.add(new ListItem(array.getJSONObject(a).getString("title"), array.getJSONObject(a).getString("content"), array.getJSONObject(a).getString("uid")));
        }

        for (int a = 0; a < newList.size(); a++) {
            if(!list.contains(newList.get(a))){
                list.add(newList.get(a));
                notifyItemInserted(a);
            };
        }

        for (int a = 0; a < list.size(); a++) {
            if(!newList.contains(newList.get(a))){
                list.remove(newList.get(a));
                notifyItemRemoved(a);
            };
        }
    }

    public class ListItem {
        String title, content,uid;

        public ListItem(String title, String content,String uid) {
            this.title = title;
            this.content = content;
            this.uid = uid;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ListItem)) return false;
            ListItem item = (ListItem) obj;
            return title.equals(item.title) && content.equals(item.content);
        }
    }

    public class VH extends RecyclerView.ViewHolder {
        TextView title, content;

        public VH(View v) {
            super(v);
            title = (TextView) v.findViewById(R.id.title);
            content = (TextView) v.findViewById(R.id.content);
        }
    }

    public ListAdapter(Context context) {
        this.context = context;
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
        ListItem item = list.get(position);

        holder.title.setText(item.title);
        holder.content.setText(item.content);
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(context).inflate(R.layout.list_item, parent, false));
    }
}
