package zixing.bluetooth.unlocker.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import zixing.bluetooth.unlocker.utils.SPUtils;

public class SettingProvider  extends ContentProvider {

    private Context mContext;
    private static final int QUEYSUCESS = 0;
    private static final int INSERTSUCESS = 1;
    //UriMatcher.NO_MATCH表示不匹配任何路径的返回码
    private static UriMatcher mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private SQLiteDatabase mDb;

    static {
        //注册所有要匹配的uri
        mUriMatcher.addURI("zixing.bluetooth.unlocker.provider.SettingProvider", "query", QUEYSUCESS);
        //mUriMatcher.addURI("zixing.bluetooth.unlocker.provider.SettingProvider", "insert", INSERTSUCESS);
    }

    //该方法在其它应用第一次访问它时才会被创建
    @Override
    public boolean onCreate() {
        mContext = getContext();
        SPUtils.getInstance().init(mContext);
        return false;
    }

    /**
     * public final Cursor query (Uri uri, String[] projection,String selection,String[] selectionArgs, String sortOrder)
     * projection ： 这个参数告诉查询要返回的列（Column）即需要的字段，比如Contacts Provider提供了联系人的ID和联系人的NAME等内容.
     * selection ：查询where字句
     * selectionArgs ： 查询条件属性值
     * sortOrder ：结果排序
     */
    @Override
    public Cursor query(Uri uri, String[] arg1, String arg2, String[] arg3, String arg4) {
        if (mUriMatcher.match(uri) == QUEYSUCESS) {//uri匹配后进行下面的操作
            MatrixCursor cursor = new MatrixCursor(new String[]{"data"});
            String data = SPUtils.getString(arg1[0],arg1[1]);
            cursor.addRow(new String[]{data});
            getContext().getContentResolver().notifyChange(uri, null);
            return cursor;
        } else {
            throw new IllegalArgumentException("match fail");
        }
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        return 0;
    }
}