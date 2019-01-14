package io.kloudformation.module

import io.kloudformation.KloudFormation
import io.kloudformation.Value

typealias Builder<Parts> = KloudFormation.(Parts.()->Unit) -> Unit

interface Properties
object NoProps: Properties

interface Mod<T, R, P: Properties>

fun <T> Value<T>.value() = (this as Value.Of<T>).value

abstract class Modification<T, R, P: Properties>: Mod<T,R,P>{
    open var item: R? = null
    open var replaceWith: R? = null
    open var modifyBuilder: T.(P) -> T = { this }
    open var modifyProps: P.() -> Unit = {}
    operator fun invoke(modify: Modification<T, R, P>.() -> Unit){
        replaceWith = null
        run(modify)
    }
    operator fun invoke(props: P, modify: Modification<T, R, P>.(P)->R): R{
        return if(replaceWith != null) replaceWith!! else {
            item = modify(props(props))
            return item!!
        }
    }
    fun modify(mod: T.(P) -> Unit){ modifyBuilder = { mod(it); this } }
    fun props(mod: P.() -> Unit){ modifyProps = mod }
    fun props(defaults: P) = defaults.apply(modifyProps)
    fun replaceWith(item: R){ replaceWith = item }
}

abstract class OptionalModification<T, R, P: Properties>(private var remove: Boolean = false): Mod<T,R,P>{
    open var item: R? = null
    open var replaceWith: R? = null
    open var modifyBuilder: T.(P) -> T = { this }
    open var modifyProps: P.() -> Unit = {}
    operator fun invoke(modify: OptionalModification<T, R, P>.() -> Unit) {
        replaceWith = null
        remove = false
        run(modify)
    }
    operator fun invoke(props: P, modify: OptionalModification<T, R, P>.(P)->R): R?{
        return if(!remove){
            if(replaceWith != null) replaceWith else {
                item = modify(props(props))
                item
            }
        } else item
    }
    fun modify(mod: T.(P) -> T){ modifyBuilder = { mod(it); this } }
    fun props(mod: P.() -> Unit){ modifyProps = mod }
    fun props(defaults: P) = defaults.apply(modifyProps)
    fun remove(){ remove = true }
    fun keep(){ remove = false }
    fun replaceWith(item: R){ replaceWith = item }
}

fun <Builder, R, P: Properties> modification() = object: Modification<Builder, R, P>(){}
fun <Builder, R, P: Properties> optionalModification(absent: Boolean = false) = (object: OptionalModification<Builder, R, P>(){}).apply {
    if(absent) remove()
}

fun <Builds: Module, Parts> builder(builder: ModuleBuilder<Builds, Parts>): Builder<Parts> = {
    builder.run { create(it) }
}
fun <Builds: Module, Parts, Predefined: Properties, Props: Properties> KloudFormation.builder(builder: SubModuleBuilder<Builds, Parts, Predefined, Props>, partBuilder: Parts.()->Unit): Builds =
        builder.run { create(partBuilder) }
fun <Builds: Module, Parts> KloudFormation.builder(builder: ModuleBuilder<Builds, Parts>, partBuilder: Parts.()->Unit): Builds =
        builder.run { create(partBuilder) }

interface Module

abstract class ModuleBuilder<Builds: Module, Parts>(val parts: Parts){
    fun KloudFormation.create(builder: Parts.()->Unit): Builds {
        return parts.apply(builder).run(buildModule())
    }
    abstract fun KloudFormation.buildModule(): Parts.() -> Builds
}

abstract class SubModuleBuilder<Builds: Module, Parts, Predefined: Properties, Props: Properties>(val pre: Predefined, parts: Parts): ModuleBuilder<Builds, Parts>(parts)


class SubModules<Builds: Module, Parts, Predefined: Properties, UserProps: Properties>(
        val builder: (Predefined, UserProps) -> SubModuleBuilder<Builds, Parts, Predefined, UserProps>,
        private val subModules: MutableList<SubModule<Builds, Parts, Predefined, UserProps>> = mutableListOf()
){
    operator fun invoke(props: UserProps, modifications: Modification<Parts,Builds,Predefined>.() -> Unit = {}){
        val module: SubModule<Builds, Parts, Predefined, UserProps> = SubModule(builder)
        module(props, modifications)
        subModules.add(module)
    }
    fun modules(): List<SubModule<Builds, Parts, Predefined, UserProps>> = subModules
}

class SubModule<Builds: Module, Parts, Predefined: Properties, UserProps: Properties>(
        val builder: (Predefined, UserProps) -> SubModuleBuilder<Builds, Parts, Predefined, UserProps>,
        private var modification: Modification<Parts,Builds, Predefined> = modification(),
        private var subModule: (KloudFormation.(Predefined) -> Builds)? = null
){

    fun module(pre: Predefined): KloudFormation.() -> Builds? = {
        subModule?.invoke(this,pre)
    }
    operator fun KloudFormation.invoke(pre: Predefined): Builds? = subModule?.invoke(this, pre)

    operator fun invoke(props: UserProps, modifications: Modification<Parts,Builds,Predefined>.() -> Unit = {}){
        subModule = { pre ->
            modification(pre) { preProps ->
                apply(modifications)
                modifyProps(preProps)
                with(builder(preProps,props)){
                    this.parts.modifyBuilder(preProps)
                    create { }
                }
            }
        }
    }
}