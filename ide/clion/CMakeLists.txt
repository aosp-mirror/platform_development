# Will add_directory the given path only if the directory exists. If the directory doesn't exist,
# it will look for either arm64-android or x86_64-android variants of the path. If it finds either, or both,
# then it will add the newer of the two.
function(try_add_subdir path)
    if(IS_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/${path}")
      add_subdirectory(${path})
    endif()

    set(path_arm64 ${path}-arm64-android)
    if (IS_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/${path_arm64}")
        set(has_arm64 TRUE)
    else()
        set(has_arm64 FALSE)
    endif()

    set(path_x64 ${path}-x86_64-android)
    if (IS_DIRECTORY "${CMAKE_CURRENT_SOURCE_DIR}/${path_x64}")
        set(has_x64 TRUE)
    else()
        set(has_x64 FALSE)
    endif()

    if (${has_arm64} AND ${has_x64})
        if ("${CMAKE_CURRENT_SOURCE_DIR}/${path_arm64}/CMakeLists.txt" IS_NEWER_THAN
                "${CMAKE_CURRENT_SOURCE_DIR}/${path_x64}/CMakeLists.txt")
            add_subdirectory(${path_arm64})
        else()
            add_subdirectory(${path_x64})
        endif()
    elseif(${has_arm64})
        add_subdirectory(${path_arm64})
    elseif(${has_x64})
        add_subdirectory(${path_x64})
    endif()
endfunction()