/**
 * Injects all the properties (getters, setters, functions...) of a list of
 * classes (baseCtors) into a class (derivedCtor).
 * @param derivedCtor The constructor of the class we want to inject the
 *                    properties into.
 * @param baseCtors A list of consturctor of the classes we want to mixin the
 *                  properties of into the derivedCtor.
 */
export function applyMixins(derivedCtor: any, baseCtors: any[]) {
  baseCtors.forEach(baseCtor => {
    Object.getOwnPropertyNames(baseCtor).forEach(name => {
      if (['length', 'name', 'prototype'].includes(name)) {
        return;
      }

      Object.defineProperty(derivedCtor, name, Object.getOwnPropertyDescriptor(baseCtor, name))
    })

    Object.getOwnPropertyNames(baseCtor.prototype).forEach(name => {
      Object.defineProperty(derivedCtor.prototype, name, Object.getOwnPropertyDescriptor(baseCtor.prototype, name))
    })
  });
}