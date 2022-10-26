package com.example.travelblog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.SearchView;
import android.widget.Toast;

import com.example.travelblog.Adapter.PostAdapter;
import com.example.travelblog.Model.PostModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class HomeActivity<onCreateOp> extends AppCompatActivity {

    FirebaseAuth auth;
    RecyclerView recyclerView;
    PostAdapter postAdapter;
    List<PostModel> postModelList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        auth= FirebaseAuth.getInstance();
        recyclerView = findViewById(R.id.recyclerView);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);

        recyclerView.setLayoutManager(layoutManager);

        postModelList = new ArrayList<>();
        loadPosts();

    }

    private void loadPosts() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postModelList.clear();
                for (DataSnapshot ds: snapshot.getChildren()){
                    PostModel postModel = ds.getValue(PostModel.class);
                    postModelList.add(postModel);
                    postAdapter = new PostAdapter(HomeActivity.this, postModelList);
                    recyclerView.setAdapter(postAdapter);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                Toast.makeText(HomeActivity.this, ""+error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        MenuItem item =menu.findItem(R.id.search_bar);
        SearchView searchView=(SearchView)item.getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                postAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                postAdapter.getFilter().filter(newText);
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.action_logout ){
            auth.signOut();
            startActivity(new Intent( HomeActivity.this, MainActivity.class));
        }
        if (item.getItemId() == R.id.action_add_post) {
            startActivity(new Intent(HomeActivity.this, AddPostActivity.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finishAffinity();
    }



}
