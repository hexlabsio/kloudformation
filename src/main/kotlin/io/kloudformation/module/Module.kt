package io.kloudformation.module

import io.kloudformation.KloudFormation
import io.kloudformation.Value

typealias Builder<Builds, Parts> = KloudFormation.(Parts.() -> Unit) -> Builds

abstract class Parts(private val items: MutableList<Any> = mutableListOf()) : List<Any> by items {
    val parts: List<Any> = items
    private fun <T : Properties> T.withParts() = also { it.items.addAll(this@Parts.items) }
    inline fun <reified T> get() = parts.filter { it is T }.map { it as T }
    inline fun <reified T> first() = parts.first { it is T } as T
    inline fun <reified T> firstOrNull() = parts.firstOrNull { it is T } as? T
    inline fun <reified T> last() = parts.last { it is T } as T
    inline fun <reified T> lastOrNull() = parts.lastOrNull { it is T } as? T
    fun <T, R, P : Properties> Modification<T, R, P>.build(props: P, modify: Modification<T, R, P>.(P) -> R): R =
            invoke(props.withParts(), modify).also { if (it != null) items.add(it) }

    fun <Builds : Module, P : Parts, Predefined : Properties, UserProps : Properties>
            KloudFormation.build(submodule: SubModule<Builds, P, Predefined, UserProps>, pre: Predefined): Builds? =
            with(submodule) { module(pre.withParts()) }.also { if (it != null) items.add(it) }
}

abstract class Properties(internal val items: MutableList<Any?> = mutableListOf()) {
    val parts: List<Any?> = items
    inline fun <reified T> get() = parts.filter { it is T }.map { it as T }
    inline fun <reified T> first() = parts.first { it is T } as T
    inline fun <reified T> firstOrNull() = parts.firstOrNull { it is T } as? T
    inline fun <reified T> last() = parts.last { it is T } as T
    inline fun <reified T> lastOrNull() = parts.lastOrNull { it is T } as? T
}
object NoProps : Properties()

fun <T> Value<T>.value() = (this as Value.Of<T>).value

abstract class Modification<T, R, P : Properties> {
    open var item: R? = null
    open var replaceWith: R? = null
    open var modifyBuilder: T.(P) -> T = { this }
    open var modifyProps: P.() -> Unit = {}
    // User hits this
    open operator fun invoke(modify: T.(P) -> Unit): () -> R {
        replaceWith = null
        modifyBuilder = { modify(it); this }
        return { item!! }
    }
    // Builder hits this
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
    override operator fun invoke(modify: T.(P) -> Unit): () -> R? {
        keep()
        return super.invoke(modify)
    }
    override fun invoke(props: P, modify: Modification<T, R?, P>.(P) -> R?): R? =
            if (!remove) super.invoke(props, modify) else null
}

fun <Builder, R, P : Properties> modification() = object : Modification<Builder, R, P>() {}
fun <Builder, R, P : Properties> optionalModification(absent: Boolean = false) = (object : OptionalModification<Builder, R, P>() {}).apply {
    if (absent) remove()
}

fun <Builds : Module, P : Parts> builder(builder: ModuleBuilder<Builds, P>): Builder<Builds, P> = {
    builder.run { create(it) }
}
fun <Builds : Module, P : Parts, Predefined : Properties> KloudFormation.builder(builder: SubModuleBuilder<Builds, P, Predefined>, partBuilder: P.() -> Unit): Builds =
        builder.run { create(partBuilder) }
fun <Builds : Module, P : Parts> KloudFormation.builder(builder: ModuleBuilder<Builds, P>, partBuilder: P.() -> Unit): Builds =
        builder.run { create(partBuilder) }

interface Module

abstract class ModuleBuilder<Builds : Module, P : Parts>(val parts: P) {
    fun KloudFormation.create(builder: P.() -> Unit): Builds {
        return parts.apply(builder).run(buildModule())
    }
    abstract fun KloudFormation.buildModule(): P.() -> Builds
}

abstract class SubModuleBuilder<Builds : Module, P : Parts, Predefined : Properties>(val pre: Predefined, parts: P) : ModuleBuilder<Builds, P>(parts)

class NoPropsSubModules<Builds : Module, P : Parts, Predefined : Properties>(
    builder: (Predefined) -> SubModuleBuilder<Builds, P, Predefined>
) : SubModules<Builds, P, Predefined, NoProps>({ predefined, _ -> builder(predefined) }) {
    operator fun invoke(modifications: P.(Predefined) -> Unit = {}) {
        super.invoke(NoProps, modifications)
    }
}

open class SubModules<Builds : Module, P : Parts, Predefined : Properties, UserProps : Properties>(
    val builder: (Predefined, UserProps) -> SubModuleBuilder<Builds, P, Predefined>,
    private val subModules: MutableList<SubModule<Builds, P, Predefined, UserProps>> = mutableListOf()
) {
    operator fun invoke(props: UserProps, modifications: P.(Predefined) -> Unit = {}) {
        val module: SubModule<Builds, P, Predefined, UserProps> = SubModule(builder)
        module(props, modifications)
        subModules.add(module)
    }
    fun modules(): List<SubModule<Builds, P, Predefined, UserProps>> = subModules
}

class NoPropsSubModule<Builds : Module, P : Parts, Predefined : Properties>(
    builder: (Predefined) -> SubModuleBuilder<Builds, P, Predefined>
) : SubModule<Builds, P, Predefined, NoProps>({ predefined, _ -> builder(predefined) }) {
    operator fun invoke(modifications: P.(Predefined) -> Unit = {}) = super.invoke(NoProps, modifications)
}

open class SubModule<Builds : Module, P : Parts, Predefined : Properties, UserProps : Properties>(
    val builder: (Predefined, UserProps) -> SubModuleBuilder<Builds, P, Predefined>,
    private var modification: Modification<P, Builds, Predefined> = modification(),
    private var subModule: (KloudFormation.(Predefined) -> Builds)? = null
) {

    fun KloudFormation.module(pre: Predefined): Builds? = subModule?.invoke(this, pre)

    operator fun KloudFormation.invoke(pre: Predefined): Builds? = subModule?.invoke(this, pre)

    operator fun invoke(props: UserProps, modifications: P.(Predefined) -> Unit = {}): () -> Builds {
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
        return { modification.item!! }
    }
}

fun <Builds : Module, P : Parts, Predefined : Properties, UserProps : Properties> submodule(
    builder: (Predefined, UserProps) -> SubModuleBuilder<Builds, P, Predefined>
) = SubModule(builder)
fun <Builds : Module, P : Parts, Predefined : Properties, UserProps : Properties> submodules(
    builder: (Predefined, UserProps) -> SubModuleBuilder<Builds, P, Predefined>
) = SubModules(builder)
fun <Builds : Module, P : Parts, Predefined : Properties> submodule(
    builder: (Predefined) -> SubModuleBuilder<Builds, P, Predefined>
) = NoPropsSubModule(builder)
fun <Builds : Module, P : Parts, Predefined : Properties> submodules(
    builder: (Predefined) -> SubModuleBuilder<Builds, P, Predefined>
) = NoPropsSubModules(builder)