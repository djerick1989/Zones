package org.anhonesteffort.polygons;

import java.util.List;

import org.anhonesteffort.polygons.R;
import org.anhonesteffort.polygons.geometry.TaggedPoint;
import org.anhonesteffort.polygons.geometry.TaggedPolygon;
import org.anhonesteffort.polygons.map.PolygonMapActivity;
import org.anhonesteffort.polygons.storage.DatabaseHelper;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class PolygonListActivity extends SherlockActivity implements
  ListView.OnItemClickListener, ListView.OnItemLongClickListener, ActionMode.Callback {
  
  private static final String TAG = "org.anhonesteffort.polygons.PolygonListActivity";
  public static final String RESTORE_SELECTIONS = "org.anhonesteffort.polygons.TEST";
  private ListView polygonList;
  private ActionMode mActionMode;
  private DatabaseHelper applicationStorage;
  private AlertDialog editLabelDialog;
  private int select_count;
  private boolean select_mode;

  private void initializeList() {
    Log.d(TAG, "initializeList()");
    List<TaggedPolygon<TaggedPoint>> polygons = applicationStorage.polygons.getPolygons();
    ArrayAdapter<TaggedPolygon<TaggedPoint>> adapter = new PolygonArrayAdapter(this, R.layout.polygon_row_layout, polygons);

    polygonList = (ListView) findViewById(R.id.list);
    polygonList.setAdapter(adapter);
    polygonList.setOnItemClickListener(this);
    polygonList.setOnItemLongClickListener(this);

    select_count = 0;
    select_mode = false;
  }

  public void showEditLabelDialog() {
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    LayoutInflater inflater = this.getLayoutInflater();
    final View view = inflater.inflate(R.layout.new_polygon_label_layout, null);

    builder.setView(view).setTitle(R.string.title_polygon_label_dialog);
    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        EditText polygonLabelEdit = (EditText) view.findViewById(R.id.polygon_label);
        List<TaggedPolygon<TaggedPoint>> selectedPolygons = applicationStorage.polygons.getPolygonsSelected();
        if(selectedPolygons.get(0) != null) {
          selectedPolygons.get(0).setLabel(polygonLabelEdit.getText().toString());
          applicationStorage.polygons.updatePolygon(selectedPolygons.get(0));
          mActionMode.finish();
          initializeList();
        }
      }
    });
    editLabelDialog = builder.show();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.d(TAG, "onCreate()");
    super.onCreate(savedInstanceState);
    setContentView(R.layout.polygon_list_layout);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setSubtitle(R.string.menu_polygon_list);

    applicationStorage = DatabaseHelper.getInstance(this.getBaseContext());
    if(this.getIntent().getBooleanExtra(RESTORE_SELECTIONS, true) == false)
      applicationStorage.polygons.clearSelectedPolygons();
  }

  @Override
  public void onPause() {
    Log.d(TAG, "onPause()");
    super.onPause();
    if(editLabelDialog != null)
      editLabelDialog.dismiss();
  }

  @Override
  public void onResume() {
    Log.d(TAG, "onResume()");
    super.onResume();
    if(this.getIntent().getBooleanExtra(RESTORE_SELECTIONS, true) == false)
      initializeList();
  }

  @Override
  protected void onRestoreInstanceState(Bundle savedInstanceState) {
    Log.d(TAG, "onRestoreInstanceState()");
    if(savedInstanceState != null && savedInstanceState.getBoolean(RESTORE_SELECTIONS, false)) {
      initializeList();
      select_count = applicationStorage.polygons.getPolygonsSelected().size();
      if(select_count > 0) {
        select_mode = true;
        mActionMode = startActionMode(this);
        updateActionMode();
      }
    }
    else
      applicationStorage.polygons.clearSelectedPolygons();
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    Log.d(TAG, "onSaveInstanceState()");
    outState.putBoolean(RESTORE_SELECTIONS, select_mode);
    this.getIntent().putExtra(RESTORE_SELECTIONS, select_mode);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {

      case android.R.id.home:
        finish();

      default:
        break;
    }
    return true;
  }

  private void updateActionMode() {
    if(select_mode == false)
      mActionMode.finish();
    else {
      mActionMode.setSubtitle(select_count + " " + getString(R.string.polygons_selected));
      mActionMode.getMenu().getItem(0).setVisible((select_count == 1));
    }
  }

  public void selectItem(View view) {
    Log.d(TAG, "selectItem()");
    select_count++;
    applicationStorage.polygons.setPolygonSelected(((Integer) view.getTag(R.integer.polygon_id_tag)).intValue(), true);
    view.setTag(R.integer.polygon_select_tag, Boolean.TRUE);
    view.setBackgroundResource(R.color.abs__holo_blue_light);
    updateActionMode();
  }

  public void unselectItem(View view) {
    Log.d(TAG, "unselectItem()");
    applicationStorage.polygons.setPolygonSelected(((Integer) view.getTag(R.integer.polygon_id_tag)).intValue(), false);
    view.setTag(R.integer.polygon_select_tag, Boolean.FALSE);
    view.setBackgroundResource(0);
    if(--select_count < 1)
      select_mode = false;
    updateActionMode();
  }

  @Override
  public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    if(view.isShown() == false)
      return;

    if(select_mode == false) {
      Integer polygon_id = (Integer) view.getTag(R.integer.polygon_id_tag);
      Intent intent = new Intent();

      intent.setClass(getApplicationContext(), PolygonMapActivity.class);
      intent.putExtra(PolygonMapActivity.SELECTED_POLYGON, polygon_id);
      intent.putExtra(PolygonMapActivity.SAVED_STATE, PolygonMapActivity.DrawState.EDIT_POLYGON.ordinal());
      intent.putExtra(PolygonMapActivity.SELECTED_POLYGON_FOCUS, true);
      startActivity(intent);
    }
    else {
      if(((Boolean) view.getTag(R.integer.polygon_select_tag)) == Boolean.TRUE)
        unselectItem(view);
      else
        selectItem(view);
    }
  }

  @Override
  public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
    if(view.isShown() == false)
      return true;

    if(select_mode == false && ((Boolean) view.getTag(R.integer.polygon_select_tag)) == Boolean.FALSE) {
      select_mode = true;
      mActionMode = startActionMode(this);
      selectItem(view);
    }
    return true;
  }

  @Override
  public boolean onCreateActionMode(ActionMode mode, Menu menu) {
    MenuInflater inflater = mode.getMenuInflater();
    inflater.inflate(R.menu.polygon_list_menu, menu);
    mode.setTitle(getString(R.string.title_polygon_list_batch));
    mode.setSubtitle(select_count + " " + getString(R.string.polygons_selected));
    return true;
  }

  @Override
  public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
    return false;
  }

  @Override
  public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
    if(item.getItemId() ==  R.id.delete_polygon_button) {
        for(TaggedPolygon<TaggedPoint> polygon : applicationStorage.polygons.getPolygonsSelected())
          applicationStorage.polygons.removePolygon(polygon.getID());
        mActionMode.finish();
        initializeList();
    }
    else if(item.getItemId() == R.id.edit_polygon_label_button)
      showEditLabelDialog();
    return false;
  }

  @Override
  public void onDestroyActionMode(ActionMode mode) {
    Log.d(TAG, "onDestroyActionMode()");
    for(int i = 0; i < polygonList.getChildCount(); i++) {
      polygonList.getChildAt(i).setBackgroundResource(0);
      polygonList.getChildAt(i).setTag(R.integer.polygon_select_tag, Boolean.FALSE);
    }

    applicationStorage.polygons.clearSelectedPolygons();
    select_count = 0;
    select_mode = false;
  }
}
