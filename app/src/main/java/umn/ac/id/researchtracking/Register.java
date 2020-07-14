package umn.ac.id.researchtracking;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class Register extends AppCompatActivity {
    //declare variable
    TextView tv_Login;
    EditText et_Name,et_Email,et_Password,et_Password2;
    Button btn_register;
    Boolean valid = true;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseUser;
    private FirebaseDatabase database;
    User obj_user;
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //assign variable value by view
        tv_Login = findViewById(R.id.text_login);
        et_Email = findViewById(R.id.et_email_regis);
        et_Name = findViewById(R.id.et_name_regis);
        et_Password = findViewById(R.id.et_password_regis);
        et_Password2 = findViewById(R.id.et_password2_regis);
        btn_register = findViewById(R.id.btn_register);
        database = FirebaseDatabase.getInstance();
        databaseUser = database.getReference("user");
        mAuth = FirebaseAuth.getInstance();


        tv_Login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent gotoLogin = new Intent(getApplicationContext(), Login.class);
                startActivityForResult(gotoLogin, 1);
            }
        });

        btn_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                valid = true;
                Log.d("Register","email : "+et_Email.getText().toString());
                Log.d("Register","nama : "+et_Name.getText().toString());
                Log.d("Register","pass : "+et_Password.getText().toString());
                Log.d("Register","pass2 : "+et_Password2.getText().toString());
                if(et_Name.getText().toString().isEmpty()){
                    et_Name.setError("Please Fill this form");
                    valid = false;
                }
                if(et_Email.getText().toString().isEmpty()){
                    et_Email.setError("Please Fill this form");
                    valid = false;
                }
                if(et_Password.getText().toString().isEmpty()){
                    et_Password.setError("Please Fill this form");
                    valid = false;
                }
                if(et_Password2.getText().toString().isEmpty()){
                    et_Password2.setError("Please Fill this form");
                    valid = false;
                }
                if(!et_Password.getText().toString().equals(et_Password2.getText().toString())){
                    et_Password2.setError("Password didn't match");
                    valid = false;
                }
                if(valid){
                    String email = et_Email.getText().toString();
                    String name = et_Name.getText().toString();
                    String password = et_Password.getText().toString();
                    obj_user = new User(email,name);
                    registerUser(et_Email.getText().toString() , et_Password.getText().toString());
                }
                Log.d("register","mauth : "+ FirebaseAuth.getInstance());
                Log.d("register","db : "+ FirebaseDatabase.getInstance());

            }
        });

    }

    private void registerUser(final String email, String password){
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            mAuth.getCurrentUser().sendEmailVerification();
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("registerUser", "createUserWithEmail:success");
                            FirebaseUser currentUser = mAuth.getCurrentUser();
                            updateUI(currentUser,email);
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("registerUser", "createUserWithEmail:failure", task.getException());
                            Toast.makeText(Register.this, "Authentication failed.", Toast.LENGTH_SHORT).show();
                        }

                        // ...
                    }
                });
    }
    private void updateUI(FirebaseUser currentUser,String email){
        //add data to database
        databaseUser.child(currentUser.getUid()).setValue(obj_user);
        //back to login pages
        Intent loginIntent = new Intent(this,Login.class);
        startActivity(loginIntent);
    }

}
