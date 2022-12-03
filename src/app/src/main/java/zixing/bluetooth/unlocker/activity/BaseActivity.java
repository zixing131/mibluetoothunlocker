package zixing.bluetooth.unlocker.activity;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import butterknife.BindView;
import butterknife.ButterKnife;
import zixing.bluetooth.unlocker.R;

public abstract class BaseActivity extends AppCompatActivity implements Toolbar.OnMenuItemClickListener {

    @BindView(R.id.toolbar)
    protected Toolbar toolbar;
    @BindView(R.id.txtCenterTitle)
    protected TextView txtCenterTitle;

    public abstract int getLayoutId();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getLayoutId());
        ButterKnife.bind(this);
        initToolbar();
    }

    protected void initToolbar(){
        setSupportActionBar(toolbar);
        toolbar.setOnMenuItemClickListener(this);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setDisplayShowTitleEnabled(false);
            ab.setHomeAsUpIndicator(R.mipmap.ic_back);
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }
        txtCenterTitle.getPaint().setFakeBoldText(true);
    }

    public void Tt(String msg) {
        Toast.makeText(this,msg,Toast.LENGTH_SHORT).show();
    }
}
