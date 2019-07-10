package com.yeeun.firenote;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.GravityCompat;
import androidx.appcompat.app.ActionBarDrawerToggle;

import android.view.MenuItem;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private FirebaseAuth firebaseAuth;
    private FirebaseUser firebaseUser;
    private FirebaseDatabase firebaseDatabase;

    private EditText etContent;
    private TextView date;

    private TextView userName, userEmail;

    private NavigationView navigationView;

    private String selectedMemoKey;

    private int KEY=0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        //파이어베이스 인증 객체 선언
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        //인증 되었는지 확인
        if(firebaseUser == null){
            Intent intent = new Intent(MainActivity.this, AuthActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        firebaseDatabase = FirebaseDatabase.getInstance();

        etContent = (EditText)findViewById(R.id.content);
        date = (TextView)findViewById(R.id.date);


        setSupportActionBar(toolbar);
        FloatingActionButton new_memo = (FloatingActionButton) findViewById(R.id.new_memo);
        FloatingActionButton save_memo = (FloatingActionButton) findViewById(R.id.save_memo);
        new_memo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                initMemo();
            }
        });
        save_memo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(selectedMemoKey == null)
                    saveMemo();
                else
                    updateMemo();
            }
        });

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);


        //## 인증 정보 넣는 곳이 루트뷰가 아니고, 내비게이션 헤더 뷰에 들어있음
        View headerView = navigationView.getHeaderView(0);
        userName = (TextView)headerView.findViewById(R.id.userName);
        userEmail = (TextView)headerView.findViewById(R.id.userEmail);
        profileUpdate();
        displayMemo();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_delete) {
            deleteMemo();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    //선택된 메모에 대한 처리
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Memo selectedMemo = (Memo)item.getActionView().getTag();
        etContent.setText(selectedMemo.getTxt());
        if(selectedMemo.getCreateDate()!=null) {
            int yy = selectedMemo.getCreateDate().getYear() % 100;
            int mm = selectedMemo.getCreateDate().getMonth();
            int dd = selectedMemo.getCreateDate().getDay();
            int hh = selectedMemo.getCreateDate().getHours();
            int mn = selectedMemo.getCreateDate().getMinutes();
            date.setText(yy + "년 " + mm + "월 " + dd + "일 " + hh + ":" + mn + " 작성");
        }
        selectedMemoKey = selectedMemo.getKey();

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void initMemo(){
        selectedMemoKey = null;
        etContent.setText("");
        date.setText("");

    }
    private void saveMemo(){
        Memo memo = new Memo();
        String text = etContent.getText().toString();

        if(text.isEmpty()) {
            return;
        }
        memo.setTxt(text);
        memo.setCreateDate(new Date());
        DatabaseReference myRef = firebaseDatabase.getReference();
        myRef.child("memo/"+firebaseUser.getUid()).push().setValue(memo)
                .addOnSuccessListener(MainActivity.this, new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Snackbar.make(etContent, "메모가 저장되었습니다.\uD83D\uDE0D", Snackbar.LENGTH_LONG).show();
            }
        });
        initMemo();

    }

    private void updateMemo(){
        Memo memo = new Memo();
        String text = etContent.getText().toString();
        if(text.isEmpty()) {
            return;
        }
        memo.setTxt(text);
        memo.setUpdateDate(new Date());

        DatabaseReference myRef = firebaseDatabase.getReference();
        myRef.child("memo/"+firebaseUser.getUid()+"/"+selectedMemoKey).setValue(memo)
                .addOnSuccessListener(MainActivity.this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Snackbar.make(etContent, "메모가 수정되었습니다.\uD83D\uDC95", Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void deleteMemo(){
        if(selectedMemoKey == null){
            return;
        }
        Snackbar.make(etContent, "메모를 삭제하시겠습니까?", Snackbar.LENGTH_LONG).setAction("삭제", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                DatabaseReference myRef = firebaseDatabase.getReference();
                myRef.child("memo/"+firebaseUser.getUid()+"/"+selectedMemoKey).removeValue(new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable DatabaseError databaseError, @NonNull DatabaseReference databaseReference) {
                        Snackbar.make(etContent, "메모가 삭제되었습니다.\uD83D\uDDD1", Snackbar.LENGTH_LONG).show();
                    }
                });
            }
        }).show();

    }

    private void profileUpdate(){
        userName.setText(firebaseUser.getDisplayName());
        userEmail.setText(firebaseUser.getEmail());
    }

    private void displayMemo(){
        firebaseDatabase.getReference().child("memo/"+firebaseUser.getUid())
                .addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        //데이터 추가된 경우
                        Memo memo = dataSnapshot.getValue(Memo.class);
                        memo.setKey(dataSnapshot.getKey());//키값 설정
                        displayMemoList(memo);
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        //데이터 변경된 경우
                        //메뉴를 바꾸는 과정
                        Memo memo = dataSnapshot.getValue(Memo.class);
                        memo.setKey(dataSnapshot.getKey());//키값 설정
                        for(int i = 0; i < navigationView.getMenu().size(); i++)
                        {
                            MenuItem menuItem = navigationView.getMenu().getItem(i);
                            if((memo.getKey()).equals(((Memo)menuItem.getActionView().getTag()).getKey()))
                            {
                                menuItem.getActionView().setTag(memo);
                                menuItem.setTitle(memo.getTitle());
                                break;
                            }
                        }

                    }

                    @Override
                    public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                        //데이터가 삭제된 경우
                        Memo memo = dataSnapshot.getValue(Memo.class);
                        memo.setKey(dataSnapshot.getKey());//키값 설정
                        Menu leftMenu = navigationView.getMenu();
                        for(int i = 0; i < leftMenu.size(); i++)
                        {
                            MenuItem menuItem = leftMenu.getItem(i);
                            if((memo.getKey()).equals(((Memo)menuItem.getActionView().getTag()).getKey()))
                            {
                                menuItem.setVisible(false);
                                initMemo();
                                break;
                            }
                        }

                    }

                    @Override
                    public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                        //데이터가 이동된 경우->구현 X

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        //데이터가 취소된 경우->구현 X

                    }
                });
    }

//데이터가 추가된 경우에 호출될 함수
    private void displayMemoList(Memo memo){
        Menu leftMenu = navigationView.getMenu();
        MenuItem item = leftMenu.add(memo.getTitle());
        View view = new View(getApplication());
        view.setTag(memo);
        item.setActionView(view);
    }
}
