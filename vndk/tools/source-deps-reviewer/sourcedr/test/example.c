int main() {
    printf("This is a simple testing file\n");
    int dlopen_analysis = 1;
    "This line with dlopen shouldn't be found"
    /*
     * This dlopen shouldn't be found
     */
    dlopen("dlopen");
    handle = dlopen("libm.so.6", RTLD_LAZY);
}
