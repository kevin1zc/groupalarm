package edu.rhit.groupalarm.groupalarm.Adapters;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

import edu.rhit.groupalarm.groupalarm.Fragments.MainFragment;
import edu.rhit.groupalarm.groupalarm.R;
import edu.rhit.groupalarm.groupalarm.User;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

    private final int mode; //1 = friends, 0 = invitations

    private ArrayList<String> keyList;
    private ArrayList<User> friendList;

    private DatabaseReference userRef = FirebaseDatabase.getInstance().getReference().child("users");

    private Context context;

    public FriendsAdapter(int mode, Context context) {
        this.mode = mode;
        this.context = context;
        keyList = new ArrayList<>();
        friendList = new ArrayList<>();
        startMonitoring();
    }

    private void startMonitoring() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    refresh();
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        new Thread(runnable).start();
    }

    private void refresh() {
        loadData();
        startQuery();
    }

    private void loadData() {
        User mUser = MainFragment.getCurrentUserInstance();
        keyList.clear();
        HashMap<String, Boolean> map = mUser.getmFriendList();
        for (String key : map.keySet()) {
            if (key.equals(mUser.getmUid())) {
                continue;
            }
            if (mode == 1) { //friends
                if (map.get(key)) {
                    keyList.add(key);
                }
            } else { //invitations
                if (!map.get(key)) {
                    keyList.add(key);
                }
            }
        }
    }

    private void startQuery() {
        friendList.clear();
        for (String key : keyList) {
            Query query = userRef.orderByChild("mUid").equalTo(key);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot childSnapshot : dataSnapshot.getChildren()) {
                        User user = childSnapshot.getValue(User.class);
                        friendList.add(user);
                    }
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            notifyDataSetChanged();
                        }
                    });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        if (mode == 1) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_list, parent, false);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_request, parent, false);
        }
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = friendList.get(position);
        holder.nameTextView.setText(user.getmUsername());
        if (mode == 1) {
            ImageView imageView = holder.friendStatusView;
            imageView.setImageResource(R.drawable.ic_alarm);
            if (user.ismIsAwake()) {
                imageView.setColorFilter(ContextCompat.getColor(context, R.color.green));
            } else {
                imageView.setColorFilter(ContextCompat.getColor(context, R.color.red));
            }
        } else {
            //TODO: two buttons
        }
    }

    @Override
    public int getItemCount() {
        Log.d("AdapterTest", friendList.size() + "");
        return friendList.size();
    }

    private void queryUser(String username, final boolean isAccept) {
        Query query = userRef.orderByChild("mUsername").equalTo(username);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()) {
                    User user = child.getValue(User.class);
                    acceptOrReject(user, isAccept);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void acceptOrReject(User user, boolean isAccept) {
        if (isAccept) {
            HashMap<String, Boolean> otherFriendList = user.getmFriendList();
            otherFriendList.put(MainFragment.getCurrentUserInstance().getmUid(), true);
            userRef.child(user.getmUid()).child("mFriendList").setValue(otherFriendList);

            HashMap<String, Boolean> mFriendList = MainFragment.getCurrentUserInstance().getmFriendList();
            mFriendList.put(user.getmUid(), true);
            userRef.child(MainFragment.getCurrentUserInstance().getmUid()).child("mFriendList").setValue(mFriendList);
        } else {
            HashMap<String, Boolean> mFriendList = MainFragment.getCurrentUserInstance().getmFriendList();
            mFriendList.remove(user.getmUid());
            userRef.child(MainFragment.getCurrentUserInstance().getmUid()).child("mFriendList").setValue(mFriendList);
        }
        refresh();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private TextView nameTextView;
        private ImageView friendStatusView;
        private Button acceptButton;
        private Button rejectButton;

        public ViewHolder(final View itemView) {
            super(itemView);
            if (mode == 1) {
                nameTextView = itemView.findViewById(R.id.friend_name_textview);
                friendStatusView = itemView.findViewById(R.id.friend_status_imageview);
            } else {
                nameTextView = itemView.findViewById(R.id.new_friend_name_textView);
                acceptButton = itemView.findViewById(R.id.accept_button);
                rejectButton = itemView.findViewById(R.id.reject_button);
                acceptButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        queryUser(nameTextView.getText().toString(), true);
                        Snackbar.make(itemView, "Request Accepted", Snackbar.LENGTH_SHORT).show();
                    }
                });
                rejectButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        queryUser(nameTextView.getText().toString(), false);
                        Snackbar.make(itemView, "Request Declined", Snackbar.LENGTH_SHORT).show();
                    }
                });
            }
        }
    }

}
