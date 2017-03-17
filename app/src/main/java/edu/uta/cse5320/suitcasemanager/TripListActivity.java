package edu.uta.cse5320.suitcasemanager;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import edu.uta.cse5320.dao.BagAdapter;
import edu.uta.cse5320.dao.BagData;
import edu.uta.cse5320.dao.TripAdapter;
import edu.uta.cse5320.dao.TripData;
import edu.uta.cse5320.dao.TripHelper;

public class TripListActivity extends AppCompatActivity {


    private Button mLogoutBtn;
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private GoogleApiClient mGoogleApiClient;
    private Context ctx;
    private EditText editTextName, editTextPhone, editTextAge;
    private DatabaseReference myDbRef;
    private TextView mTextViewName, mTextViewAge;
    //List<String> tripArray ;
    HashMap<String, String> hmap = new HashMap<String, String>();
    FirebaseUser user;
    private ListView listViewTrip;
    String TAG = "Suitcase Manager::TripScreen";
    public static final String EXTRA_MESSAGE = "edu.uta.cse5320.MESSAGE";
    ArrayAdapter<String> myAdapter;
    TripAdapter myTripAdapter;
    ArrayList<TripData> tripDataList;
    TripHelper tripHelperDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trip_list);
        //tripArray = new ArrayList<String>();

        mLogoutBtn = (Button) findViewById(R.id.logoutBtn);

        mAuth = FirebaseAuth.getInstance();
        ctx = getApplicationContext();
        user = mAuth.getCurrentUser();

        tripDataList = new ArrayList<>();
        tripHelperDB = new TripHelper(this);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        myDbRef = database.getReference("test").child(user.getUid()).child("Trips");
        //dbUpdates(myDbRef);
        if(savedInstanceState == null){
            // everything else that doesn't update UI
            System.out.println("-------------- onCreate Test ----------");
        }
        System.out.println("-------------- onCreate ----------");
        myDbRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                System.out.println(TAG+ " onChildAdded");
                TripData tripData = dataSnapshot.getValue(TripData.class);
                //tripArray.add(tripData.getTripName());
                //Key - Value : TripName - f_id
                hmap.put(tripData.getTripName(), dataSnapshot.getKey());

                /* Inserting data in DB only when not existed*/

                int count = tripHelperDB.getListContent(tripData.getId());
                if(count <= 0){
                    System.out.println("Not Inserted!! Inserting Now");
                    tripHelperDB.addDataCompleteSync(tripData.getId(), tripData.getTripName(), tripData.getTripStartDate(), tripData.getTripEndDate(),tripData.getTripAirlineName(),tripData.getTripDetails());
                }

                /* Adding in List */
                tripDataList.add(tripData);

                updateListView();

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {
                System.out.println(TAG + "onChildChanged" + dataSnapshot.getKey() +" Value :"+ dataSnapshot.getValue().toString());
                TripData tripData = dataSnapshot.getValue(TripData.class);
                if(tripData != null){
                    //String tripName = tripData.getTripName();
                    Object oldKey = getKeyFromValue(hmap, dataSnapshot.getKey());
                    //Key - Value : TripName - f_id
                    hmap.remove(oldKey);
                    hmap.put(tripData.getTripName(), dataSnapshot.getKey());
//                    int idx=0;
//                    while (idx < tripArray.size())  {
//                        if(tripArray.get(idx).equalsIgnoreCase(tripName)) {
//                            System.out.println(TAG+" Matched ");
//                            tripArray.remove(idx);
//                        } else
//                            ++idx;
//                    }
//                    tripArray.add(tripName);

                    // Refactor the above code
                    /* Updating data in DB*/
                    boolean flag = tripHelperDB.updateDetails(tripData.getId(), tripData.getTripName(), tripData.getTripStartDate(), tripData.getTripEndDate(),tripData.getTripAirlineName(),tripData.getTripDetails());
                    //lastTouchedImageView
                    System.out.println(TAG + "flag ="+ flag );
                    /* updating in List */
                    if(flag){
                        Iterator<TripData> itr = tripDataList.iterator();
                        while (itr.hasNext()) {
                            TripData element = itr.next();
                            if(element.getId() == tripData.getId()) {
                                tripDataList.remove(element);
                                break;
                            }
                        }
                        tripDataList.add(tripData);
                        myTripAdapter.notifyDataSetChanged();
                        updateListView();
                    }
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                System.out.println(TAG + " onChildRemoved");
                TripData tripData = dataSnapshot.getValue(TripData.class);

                //Update Database and List Here
                boolean flag = tripHelperDB.deleteContent(tripData.getId());

                if(flag){
                    hmap.remove(tripData.getTripName());
                    System.out.println("Whats is flag : "+ flag);
                    Iterator<TripData> itr = tripDataList.iterator();
                    while (itr.hasNext()) {
                        TripData element = itr.next();
                        if(element.getId() == tripData.getId()) {
                            tripDataList.remove(element);
                            break;
                        }
                    }

                    updateListView();
                    myTripAdapter.notifyDataSetChanged();
                }
//                int idx=0;
//                while (idx < tripArray.size())  {
//                    if(tripArray.get(idx).equalsIgnoreCase(tripName))
//                        tripArray.remove(idx);
//                    else
//                        ++idx;
//                }
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

        /* List Trip Data */
        listViewTrip = (ListView) findViewById(R.id.listTrips);
        // initiate the listadapter
        myTripAdapter = new TripAdapter(this, R.layout.trip_list_layout, tripDataList);
        myTripAdapter.setNotifyOnChange(true);
        listViewTrip.setAdapter(myTripAdapter);

        // Configure Google Sign In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // [END config_signin]

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,  new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Toast.makeText(ctx, "Failed Connection..."+ result.hasResolution(), Toast.LENGTH_SHORT).show();
                    }
                })
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        mAuthListener = new FirebaseAuth.AuthStateListener(){
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth){
                if( firebaseAuth.getCurrentUser() == null){
                    startActivity(new Intent(TripListActivity.this, MainActivity.class));
                }
            }
        };
        // Logout Listener
        mLogoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAuth.signOut();
                Auth.GoogleSignInApi.signOut(mGoogleApiClient);
            }
        });

        //Save on Cloud
//        mSaveButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                saveUserInformation();
//            }
//        });
        /* Floating Button for moving to Add Trip */
        FloatingActionButton myFab = (FloatingActionButton) findViewById(R.id.floatingButtonAddTrip);
        myFab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(ctx, AddTripActivity.class);
                startActivity(intent);
            }
        });

        listViewTrip.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final TextView tv = (TextView) view.findViewById(R.id.tripListLabelName);
                final ImageView imageViewDelete = (ImageView) view.findViewById(R.id.imageViewDelete);
                final ImageView imageViewEdit = (ImageView) view.findViewById(R.id.imageViewEdit);
                tv.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //int position = listViewTrip.getPositionForView((View) v.getParent());
                        Toast.makeText(ctx, "Clicked on  - "+ tv.getText().toString(), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ctx, BagListActivity.class);
                        intent.putExtra(EXTRA_MESSAGE, hmap.get(tv.getText().toString()));
                        startActivity(intent);
                    }
                });
                imageViewDelete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ViewGroup row = (ViewGroup) v.getParent();
                        TextView textView = (TextView) row.findViewById(R.id.tripListLabelName);
                        String key = hmap.get(tv.getText().toString());
                        System.out.println(TAG+" Icon of  - "+ tv.getText().toString() +" Delete :"+key);
                        if(!key.isEmpty()){
                            myDbRef.child(key).setValue(null);
                        }

                    }
                });
                imageViewEdit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //int position = listViewTrip.getPositionForView((View) v.getParent());
                        ViewGroup row = (ViewGroup) v.getParent();
                        TextView textView = (TextView) row.findViewById(R.id.tripListLabelName);
                        Toast.makeText(ctx, "Clicked on  - "+ textView.getText().toString(), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(ctx, AddTripActivity.class);
                        intent.putExtra(EXTRA_MESSAGE, hmap.get(textView.getText().toString()));
                        startActivity(intent);
                    }
                });
            }
        });
    }

    @Override
    protected void onStart(){
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    private void updateListView(){
        myTripAdapter.notifyDataSetChanged();
        listViewTrip.invalidate();
    }

    public static Object getKeyFromValue(Map hm, Object value) {
        for (Object o : hm.keySet()) {
            if (hm.get(o).equals(value)) {
                return o;
            }
        }
        return null;
    }
/*
    public void saveUserInformation(){
        String name = editTextName.getText().toString();
        String age =  editTextAge.getText().toString();
        String phone =  editTextPhone.getText().toString();

        UserData userData = new UserData(name, age, phone, null); //email set to null in begining

        FirebaseUser user = mAuth.getCurrentUser();
        myDbRef.child(user.getUid()).setValue(userData);

        Toast.makeText(ctx, "Data Saved..", Toast.LENGTH_SHORT).show();
        dbUpdates(myDbRef);
    }

    public void dbUpdates(DatabaseReference myDbRef){
        myDbRef.addChildEventListener(new ChildEventListener(){
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String prevChildKey) {
                UserData ud= dataSnapshot.getValue(UserData.class);
//                mTextViewName.setText(ud.getFullName());
//                mTextViewAge.setText(ud.getAge());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {}

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String prevChildKey) {}

            @Override
            public void onCancelled(DatabaseError databaseError) {}

        });
    }*/
}
