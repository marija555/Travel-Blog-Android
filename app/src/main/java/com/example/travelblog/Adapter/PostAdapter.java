package com.example.travelblog.Adapter;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.ContactsContract;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.travelblog.HomeActivity;
import com.example.travelblog.Model.PostModel;
import com.example.travelblog.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;


public class PostAdapter extends RecyclerView.Adapter<PostAdapter.MyHolder>  implements Filterable {

    Context context;
    List<PostModel> postModelList;
    List<PostModel> postModelListFilter;
    String myUid;


    public PostAdapter(Context context, List<PostModel> postModelList) {
        this.context = context;
        this.postModelList = postModelList;
        this.postModelListFilter= postModelList;
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.home_post, parent, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyHolder holder, int position) {

            final String title = postModelListFilter.get(position).getpTitle();
            final String description = postModelListFilter.get(position).getpDescription();
            final String image= postModelListFilter.get(position).getpImage();

            holder.postTitle.setText(title);
            holder.postDescription.setText(description);

            if(myUid.equals("la9JHraOGKVX0ubk2quiqstZIbv1")){
                holder.deletePost.setVisibility(View.VISIBLE);
            }
            else
            {
                holder.deletePost.setVisibility(View.GONE);
            }

        Glide.with(context).load(image).into(holder.postImage);
        holder.postImage.setVisibility(View.VISIBLE);

        holder.deletePost.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View v) {
                showMoreOptions(holder.deletePost, myUid, image, title,description);
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void showMoreOptions(ImageButton deletePost, String myUid, final String image, final String title, final String description) {

        final PopupMenu popupMenu= new PopupMenu(context, deletePost, Gravity.END);

            if(myUid.equals("la9JHraOGKVX0ubk2quiqstZIbv1")){
                popupMenu.getMenu().add(Menu.NONE, 0, 0, "Delete");
            }

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if(id==0){
                    beginDelete(image);
                }
                return false;
            }
        });
            popupMenu.show();
    }

    private void beginDelete(final String image) {

        final ProgressDialog pd= new ProgressDialog(context);
        pd.setMessage("Deleting...");

        StorageReference picref= FirebaseStorage.getInstance().getReferenceFromUrl(image);

        picref.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {

                      Query fquery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pImage").equalTo(image);
                      fquery.addListenerForSingleValueEvent(new ValueEventListener() {
                          @Override
                          public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for(DataSnapshot ds: snapshot.getChildren()){
                                    ds.getRef().removeValue();
                                }
                                Toast.makeText(context,"Deleted successfully", Toast.LENGTH_SHORT).show();
                                pd.dismiss();
                          }

                          @Override
                          public void onCancelled(@NonNull DatabaseError error) {

                          }
                      });

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        pd.dismiss();
                        Toast.makeText(context,""+e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });

    }

    @Override
    public int getItemCount() {

        return postModelListFilter.size();
    }

    @Override
    public Filter getFilter() {
        return  new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                String character = constraint.toString();
                if(character.isEmpty()){
                    postModelListFilter= postModelList;
                }else {
                    List<PostModel> filterList = new ArrayList<>();
                    for(PostModel p : postModelList){
                        if(p.getpTitle().toLowerCase().contains(character.toLowerCase())){
                            filterList.add(p);
                        }
                    }
                    postModelListFilter = filterList;
                }
                FilterResults filterResults =  new FilterResults();
                filterResults.values= postModelListFilter;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                postModelListFilter= (ArrayList<PostModel>)results.values;
                notifyDataSetChanged();
            }
        };
    }

    class MyHolder extends RecyclerView.ViewHolder {

        public ImageButton deletePost;
        ImageView postImage;
        TextView postTitle, postDescription;


        public MyHolder(@NonNull View itemView) {
            super(itemView);

            postImage = itemView.findViewById(R.id.postImage);
            postTitle = itemView.findViewById(R.id.postTitle);
            postDescription = itemView.findViewById(R.id.postDescription);
            deletePost = itemView.findViewById(R.id.deleteBtn);



        }


    }



}




