package pablo.criado.albillos.doscientosymas;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.OvershootInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.StringTokenizer;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.VH> {
    private Context context;
    private ArrayList<ListItem> list = new ArrayList<>();
    private int defaultTextColor;

    public void setJSONData(JSONArray array) throws JSONException {
        ArrayList<ListItem> newList = new ArrayList<>();

        for (int a = 0; a < array.length(); a++) {
            JSONObject obj = array.getJSONObject(a);
            newList.add(new ListItem(obj.getInt("number"), obj.getInt("publish_state"), obj.getInt("likes"), obj.getString("content"), obj.getString("_id"), obj.getString("user"), obj.getBoolean("own_like")));
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

        for (int a = 0; a < newList.size(); a++) {
            int position = list.indexOf(newList.get(a));
            if (newList.get(a).changed(list.get(position))) {
                list.set(position, newList.get(a));
                notifyItemChanged(position);
            }
            if (a != position) {
                ListItem item = list.get(position);
                list.remove(position);
                list.add(a, item);
                //Collections.swap(list, position, a);
                notifyItemMoved(position, a);
            }
        }
    }

    public class ListItem {
        String content, uid, user;
        int number, publishState, likes;
        boolean ownLike;
        Date date;

        public ListItem(int number, int publishState, int likes, String content, String uid, String user, boolean ownLike) {
            this.number = number;
            this.content = content;
            this.uid = uid;
            this.likes = likes;
            this.publishState = publishState;
            this.user = user;
            this.ownLike = ownLike;
            date = new Date(Long.parseLong(uid.substring(0, 8), 16) * 1000);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof ListItem)) return false;
            return uid.equals(((ListItem) obj).uid);
        }

        public boolean changed(ListItem item) {
            return (!equals(item)) || (number != item.number) || (!content.equals(item.content)) || (!user.equals(item.user)) || (publishState != item.publishState) || (likes != item.likes) || (ownLike != item.ownLike);
        }
    }

    public class VH extends RecyclerView.ViewHolder {
        TextView title, content, subtitle, likesText;
        ImageView likesImage;

        public VH(View v) {
            super(v);
            title = (TextView) v.findViewById(R.id.title);
            content = (TextView) v.findViewById(R.id.content);
            subtitle = (TextView) v.findViewById(R.id.subtitle);
            likesText = (TextView) v.findViewById(R.id.likesTextView);
            likesImage = (ImageView) v.findViewById(R.id.likesImageView);
            defaultTextColor = title.getTextColors().getDefaultColor();
            likesImage.setColorFilter(defaultTextColor, PorterDuff.Mode.SRC_IN);
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
    public void onBindViewHolder(final VH holder, int position) {
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
        holder.likesText.setText(String.valueOf(item.likes));
        holder.likesImage.setImageResource(item.ownLike ? R.drawable.ic_favorite_black_24dp : R.drawable.ic_favorite_border_black_24dp);
        holder.likesImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListItem item = list.get(holder.getAdapterPosition());
                item.ownLike = !item.ownLike;
                if (item.ownLike) item.likes++;
                else item.likes--;

                /*AnimatorSet animatorSet = new AnimatorSet();

                ObjectAnimator rotationAnim;
                if (!dato_lista.like_personal) {
                    rotationAnim = ObjectAnimator.ofFloat(holder.btnlike, "rotation", 0f, 360f);
                } else {
                    rotationAnim = ObjectAnimator.ofFloat(holder.btnlike, "rotation", 0f, -360f);
                }
                rotationAnim.setDuration(300);
                rotationAnim.setInterpolator(new AccelerateInterpolator());

                ObjectAnimator bounceAnimX = ObjectAnimator.ofFloat(holder.btnlike, "scaleX", 0.2f, 1f);
                bounceAnimX.setDuration(300);
                bounceAnimX.setInterpolator(new OvershootInterpolator());

                ObjectAnimator bounceAnimY = ObjectAnimator.ofFloat(holder.btnlike, "scaleY", 0.2f, 1f);
                bounceAnimY.setDuration(300);
                bounceAnimY.setInterpolator(new OvershootInterpolator());

                animatorSet.play(rotationAnim);
                animatorSet.play(bounceAnimX).with(bounceAnimY).after(rotationAnim);

                animatorSet.start();*/
                ScaleAnimation scaleAnimation1 = new ScaleAnimation(.25f,1f,.25f,1f, Animation.RELATIVE_TO_SELF,.5f,Animation.RELATIVE_TO_SELF,.5f);
                /*ScaleAnimation scaleAnimation2 = new ScaleAnimation(1.2f,1,1.2f,1, Animation.RELATIVE_TO_SELF,.5f,Animation.RELATIVE_TO_SELF,.5f);
                AnimationSet animationSet = new AnimationSet(true);
                animationSet.setInterpolator(new FastOutSlowInInterpolator());
                animationSet.addAnimation(scaleAnimation1);
                animationSet.addAnimation(scaleAnimation2);
                animationSet.setDuration(300);*/
                scaleAnimation1.setInterpolator(new OvershootInterpolator());
                scaleAnimation1.setDuration(300);
                holder.likesImage.startAnimation(scaleAnimation1);

                holder.likesImage.setImageResource(item.ownLike ? R.drawable.ic_favorite_black_24dp : R.drawable.ic_favorite_border_black_24dp);
                holder.likesText.setText(String.valueOf(item.likes));
                ((MainActivity) context).setLike(item.uid, item.ownLike);
            }
        });
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(context).inflate(R.layout.list_item, parent, false));
    }
}
