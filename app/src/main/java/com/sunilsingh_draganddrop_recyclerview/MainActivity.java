package com.sunilsingh_draganddrop_recyclerview;

import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;

import adapter.My_adapter;
import drag.DragManager;
import drag.DragShadowBuilder;
import drag.DragState;

public class MainActivity extends AppCompatActivity implements My_adapter.My_adapterListener{
    RecyclerView recyclerView;
    private final PointF dragTouchPoint = new PointF();
    DragManager dragManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);
        recyclerView = (RecyclerView) findViewById(R.id.recycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
            @Override
            public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
                dragTouchPoint.set(e.getX(), e.getY());
                return false;
            }

            @Override
            public void onTouchEvent(RecyclerView rv, MotionEvent e) {

            }

            @Override
            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

            }
        });
        generateData_for_List();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_drag) {
            dragManager.setCanDrag(true);
        } else if (item.getItemId() == R.id.action_not_drag){
            dragManager.setCanDrag(false);
        }
        return super.onOptionsItemSelected(item);
    }

    private void generateData_for_List() {
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < 100 ; i++) {
            list.add("sunil"+i);
        }
        My_adapter myadapter = new My_adapter(MainActivity.this, list,this);
        recyclerView.setAdapter(myadapter);
        dragManager = new DragManager(recyclerView, myadapter);
        dragManager.setCanDrag(true);
        recyclerView.setOnDragListener(dragManager);
    }

    @Override
    public void onStartDrag(View view, String data) {
        DragState dragState = new DragState(data, data.toString());
        DragShadowBuilder dragShadowBuilder = new DragShadowBuilder(view,
                new Point(((int) (dragTouchPoint.x - view.getX())), (int) (dragTouchPoint.y - view.getY())));
        Point shadowSize = new Point();
        Point shadowTouchPoint = new Point();
        dragShadowBuilder.onProvideShadowMetrics(shadowSize, shadowTouchPoint);
        view.startDrag(null, dragShadowBuilder, dragState, 0);
    }
}
