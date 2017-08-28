int main() {
    printf("This is a simple testing filen");
    int dlopen_analysis = 1;
    "This line with dlopen shouldn't be found"
    /*
     * This dlopen shouldn't be found
     */
    dlopen("dlopen");
}
