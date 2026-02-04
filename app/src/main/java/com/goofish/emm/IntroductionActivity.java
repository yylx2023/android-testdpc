package com.goofish.emm;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.afwsamples.testdpc.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 介绍我们页面 - 使用 ViewPager 展示 iPad01-12 图片
 */
public class IntroductionActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private List<Integer> imageList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_introduction);

        // 强制竖屏
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // 设置 ActionBar 返回按钮
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("介绍我们");
        }

        // 初始化图片列表
        initImageList();

        // 初始化 ViewPager
        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(new ImagePagerAdapter());
    }

    private void initImageList() {
        imageList = new ArrayList<>();
        imageList.add(R.drawable.ipad01);
        imageList.add(R.drawable.ipad02);
        imageList.add(R.drawable.ipad03);
        imageList.add(R.drawable.ipad04);
        imageList.add(R.drawable.ipad05);
        imageList.add(R.drawable.ipad06);
        imageList.add(R.drawable.ipad07);
        imageList.add(R.drawable.ipad08);
        imageList.add(R.drawable.ipad09);
        imageList.add(R.drawable.ipad10);
        imageList.add(R.drawable.ipad11);
        imageList.add(R.drawable.ipad12);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * ViewPager 图片适配器
     */
    private class ImagePagerAdapter extends PagerAdapter {

        @Override
        public int getCount() {
            return imageList.size();
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return view == object;
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            ImageView imageView = new ImageView(IntroductionActivity.this);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            imageView.setImageResource(imageList.get(position));
            container.addView(imageView);
            return imageView;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            container.removeView((View) object);
        }
    }
}

