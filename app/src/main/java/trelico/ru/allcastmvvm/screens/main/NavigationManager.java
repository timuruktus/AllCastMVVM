package trelico.ru.allcastmvvm.screens.main;

public interface NavigationManager{

    void navigateTo(int resId);
    void navigateWithPopTo(int fragmentIdToNavigate, int fragmentIdToPop, boolean inclusive);
}
