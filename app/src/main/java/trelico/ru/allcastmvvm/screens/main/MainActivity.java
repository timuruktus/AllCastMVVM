package trelico.ru.allcastmvvm.screens.main;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.os.Bundle;

import trelico.ru.allcastmvvm.R;

public class MainActivity extends AppCompatActivity implements NavigationManager{

    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        navController = Navigation.findNavController(this, R.id.navHostFragment);
    }

    @Override
    public void navigateTo(int resId){

    }

    @Override
    public void navigateWithPopTo(int fragmentIdToNavigate, int fragmentIdToPop, boolean inclusive){

    }
}
