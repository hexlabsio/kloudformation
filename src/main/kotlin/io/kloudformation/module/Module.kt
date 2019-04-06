package io.kloudformation.module

import io.kloudformation.KloudFormation
import io.kloudformation.Value

typealias Builder<Builds, Parts> = KloudFormation.(Parts.() -> Unit) -> Builds

interface Properties
object NoProps : Properties

fun <T> Value<T>.value() = (this as Value.Of<T>).value

abstract class Modification<T, R, P : Properties> {
    open var item: R? = null
    open var replaceWith: R? = null
    open var modifyBuilder: T.(P) -> T = { this }
    open var modifyProps: P.() -> Unit = {}
    operator fun invoke(modify: T.(P) -> Unit) {
        replaceWith = null
        modifyBuilder = { modify(it); this }
    }
    open operator fun invoke(props: P, modify: Modification<T, R, P>.(P) -> R): R {
        return if (replaceWith != null) replaceWith!! else {
            item = modify(props(props))
            return item!!
        }
    }
    fun props(mod: P.() -> Unit) { modifyProps = mod }
    fun props(defaults: P) = defaults.apply(modifyProps)
    fun replaceWith(item: R) { replaceWith = item }
}
abstract class OptionalModification<T, R, P : Properties> : Modification<T, R?, P>() {
    private var remove = false
    fun remove() { remove = true }
    fun keep() { remove = false }
    override fun invoke(props: P, modify: Modification<T, R?, P>.(P) -> R?): R? =
            if (!remove) super.invoke(props, modify) else null
}

fun <Builder, R, P : Properties> modification() = object : Modification<Builder, R, P>() {}
fun <Builder, R, P : Properties> optionalModification(absent: Boolean = false) = (object : OptionalModification<Builder, R, P>() {}).apply {
    if (absent) remove()
}

fun <Builds : Module, Parts> builder(builder: ModuleBuilder<Builds, Parts>): Builder<Builds, Parts> = {
    builder.run { create(it) }
}
fun <Builds : Module, Parts, Predefined : Properties> KloudFormation.builder(builder: SubModuleBuilder<Builds, Parts, Predefined>, partBuilder: Parts.() -> Unit): Builds =
        builder.run { create(partBuilder) }
fun <Builds : Module, Parts> KloudFormation.builder(builder: ModuleBuilder<Builds, Parts>, partBuilder: Parts.() -> Unit): Builds =
        builder.run { create(partBuilder) }

interface Module

abstract class ModuleBuilder<Builds : Module, Parts>(val parts: Parts) {
    fun KloudFormation.create(builder: Parts.() -> Unit): Builds {
        return parts.apply(builder).run(buildModule())
    }
    abstract fun KloudFormation.buildModule(): Parts.() -> Builds
}

abstract class SubModuleBuilder<Builds : Module, Parts, Predefined : Properties>(val pre: Predefined, parts: Parts) : ModuleBuilder<Builds, Parts>(parts)

class NoPropsSubModules<Builds : Module, Parts, Predefined : Properties>(
    builder: (Predefined) -> SubModuleBuilder<Builds, Parts, Predefined>
) : SubModules<Builds, Parts, Predefined, NoProps>({ predefined, _ -> builder(predefined) }) {
    operator fun invoke(modifications: Parts.(Predefined) -> Unit = {}) {
        super.invoke(NoProps, modifications)
    }
}

open class SubModules<Builds : Module, Parts, Predefined : Properties, UserProps : Properties>(
    val builder: (Predefined, UserProps) -> SubModuleBuilder<Builds, Parts, Predefined>,
    private val subModules: MutableList<SubModule<Builds, Parts, Predefined, UserProps>> = mutableListOf()
) {
    operator fun invoke(props: UserProps, modifications: Parts.(Predefined) -> Unit = {}) {
        val module: SubModule<Builds, Parts, Predefined, UserProps> = SubModule(builder)
        module(props, modifications)
        subModules.add(module)
    }
    fun modules(): List<SubModule<Builds, Parts, Predefined, UserProps>> = subModules
}

class NoPropsSubModule<Builds : Module, Parts, Predefined : Properties>(
    builder: (Predefined) -> SubModuleBuilder<Builds, Parts, Predefined>
) : SubModule<Builds, Parts, Predefined, NoProps>({ predefined, _ -> builder(predefined) }) {
    operator fun invoke(modifications: Parts.(Predefined) -> Unit = {}) {
        super.invoke(NoProps, modifications)
    }
}

open class SubModule<Builds : Module, Parts, Predefined : Properties, UserProps : Properties>(
    val builder: (Predefined, UserProps) -> SubModuleBuilder<Builds, Parts, Predefined>,
    private var modification: Modification<Parts, Builds, Predefined> = modification(),
    private var subModule: (KloudFormation.(Predefined) -> Builds)? = null
) {

    fun module(pre: Predefined): KloudFormation.() -> Builds? = {
        subModule?.invoke(this, pre)
    }
    operator fun KloudFormation.invoke(pre: Predefined): Builds? = subModule?.invoke(this, pre)

    operator fun invoke(props: UserProps, modifications: Parts.(Predefined) -> Unit = {}) {
        subModule = { pre ->
            modification(pre) { preProps ->
                modification.invoke(modifications)
                modifyProps(preProps)
                with(builder(preProps, props)) {
                    this.parts.modifyBuilder(preProps)
                    create { }
                }
            }
        }
    }
}

fun <Builds : Module, Parts, Predefined : Properties, UserProps : Properties> submodule(
    builder: (Predefined, UserProps) -> SubModuleBuilder<Builds, Parts, Predefined>
) = SubModule(builder)
fun <Builds : Module, Parts, Predefined : Properties, UserProps : Properties> submodules(
    builder: (Predefined, UserProps) -> SubModuleBuilder<Builds, Parts, Predefined>
) = SubModules(builder)
fun <Builds : Module, Parts, Predefined : Properties> submodule(
    builder: (Predefined) -> SubModuleBuilder<Builds, Parts, Predefined>
) = NoPropsSubModule(builder)
fun <Builds : Module, Parts, Predefined : Properties> submodules(
    builder: (Predefined) -> SubModuleBuilder<Builds, Parts, Predefined>
) = NoPropsSubModules(builder)