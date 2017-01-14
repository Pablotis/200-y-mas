package pablo.criado.albillos.doscientosymas;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;

import java.math.BigInteger;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.VH> {
    private Context context;
    private ArrayList<ListItem> list = new ArrayList<>();
    private int defaultTextColor;

    public void setJSONData(JSONArray array) throws JSONException {
        ArrayList<ListItem> newList = new ArrayList<>();

        for (int a = 0; a < array.length(); a++) {
            newList.add(new ListItem(array.getJSONObject(a).getInt("number"), array.getJSONObject(a).getInt("publish_state"), array.getJSONObject(a).getString("content"), array.getJSONObject(a).getString("_id"), array.getJSONObject(a).getString("user")));
        }

        for (int a = 0; a < list.size(); a++) {
            if (!newList.contains(list.get(a))) {
                list.remove(a);
                notifyItemRemoved(a);
                a--;
            }
        }

        for (int a = 0; a < newList.size(); a++) {
            if (!list.contains(newList.get(a))) {
                list.add(a, newList.get(a));
                notifyItemInserted(a);
            }
        }

        for (int a = 0; a < list.size(); a++) {
            int position = newList.indexOf(list.get(a));
            if (list.get(a).changed(newList.get(position))) {
                list.set(a, newList.get(position));
                notifyItemChanged(a);
            }
            if (a != position) {
                ListItem item = list.get(a);
                list.remove(a);
                list.add(position, item);
                //Collections.swap(list, position, a);
                notifyItemMoved(a, position);
            }
        }
    }

    public class ListItem {
        String content, uid, user;
        int number, publishState;
        Date date;

        public ListItem(int number, int publishState, String content, String uid, String user) {
            this.number = number;
            this.content = content;
            this.uid = uid;
            this.publishState = publishState;
            this.user = user;
            date = new Date(Long.parseLong(uid.substring(0, 8), 16) * 1000);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ListItem)) return false;
            return uid.equals(((ListItem) obj).uid);
        }

        public boolean changed(ListItem item) {
            return (!equals(item)) || (number != item.number) || (!content.equals(item.content)) || (!user.equals(item.user)) || (publishState != item.publishState);
        }
    }

    public class VH extends RecyclerView.ViewHolder {
        TextView title, content, subtitle;

        public VH(View v) {
            super(v);
            title = (TextView) v.findViewById(R.id.title);
            content = (TextView) v.findViewById(R.id.content);
            subtitle = (TextView) v.findViewById(R.id.subtitle);
            defaultTextColor = title.getTextColors().getDefaultColor();
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
        String subtitleText;
        int subtitleColor;
        switch (item.publishState) {
            case 0:
                subtitleText = context.getString(R.string.published_by, item.user, DateFormat.getDateInstance().format(item.date), DateFormat.getTimeInstance().format(item.date));
                subtitleColor = defaultTextColor;
                break;
            case 1:
                subtitleText = context.getString(R.string.processing_valor);
                subtitleColor = defaultTextColor;
                break;
            case 2:
                subtitleText = context.getString(R.string.not_published);
                subtitleColor = ContextCompat.getColor(context, R.color.errorTextColor);
                break;
            default:
                subtitleText = "";
                subtitleColor = defaultTextColor;
                break;
        }

        holder.title.setText(item.number < 2000000000 ? context.getString(R.string.valor_num, item.number) : context.getString(R.string.valor_not_published));
        holder.content.setText(item.content);
        holder.subtitle.setText(subtitleText);
        holder.subtitle.setTextColor(subtitleColor);
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(context).inflate(R.layout.list_item, parent, false));
    }
}
