#!/usr/bin/env python3

from setuptools import setup

setup(
    name='sourcedr',
    version='1.0',
    description='Shared Libs Deps Review Tool',
    url='https://android.googlesource.com/platform/development/+'
        '/master/vndk/tools/source-deps-reviewer/',
    packages=['sourcedr'],
    package_data={
        'sourcedr': [
            'static/prism/js/prism.js',
            'static/prism/css/prism.css',
            'static/js/main.js',
            'static/css/main.css',
        ],
    },
    install_requires=['flask'],
    extras_require={
        'dev': [
            'flask_testing'
        ],
    },
    entry_points={
        'console_scripts': [
            'sourcedr = sourcedr.server:main',
        ],
    }
)
