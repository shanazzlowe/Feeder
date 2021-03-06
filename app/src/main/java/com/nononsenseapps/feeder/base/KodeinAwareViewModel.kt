package com.nononsenseapps.feeder.base

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import org.kodein.di.Kodein
import org.kodein.di.KodeinAware
import org.kodein.di.bindings.Factory
import org.kodein.di.direct
import org.kodein.di.generic.bind
import org.kodein.di.generic.factory
import org.kodein.di.generic.instance
import java.lang.reflect.InvocationTargetException

/**
 * A view model which is also kodein aware. Construct any deriving class by using the getViewModel()
 * extension function.
 */
open class KodeinAwareViewModel(override val kodein: Kodein) : AndroidViewModel(kodein.direct.instance()), KodeinAware

class KodeinAwareViewModelFactory(override val kodein: Kodein)
    : ViewModelProvider.AndroidViewModelFactory(kodein.direct.instance()), KodeinAware {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return if (KodeinAwareViewModel::class.java.isAssignableFrom(modelClass)) {
            try {
                modelClass.getConstructor(Kodein::class.java).newInstance(kodein)
            } catch (e: NoSuchMethodException) {
                throw RuntimeException("Cannot create an instance of $modelClass", e)
            } catch (e: IllegalAccessException) {
                throw RuntimeException("Cannot create an instance of $modelClass", e)
            } catch (e: InstantiationException) {
                throw RuntimeException("Cannot create an instance of $modelClass", e)
            } catch (e: InvocationTargetException) {
                throw RuntimeException("Cannot create an instance of $modelClass", e)
            }
        } else {
            super.create(modelClass)
        }
    }
}

inline fun <C, reified T : KodeinAwareViewModel> Kodein.BindBuilder.WithContext<C>.activityViewModelFactory():
        Factory<C, FragmentActivity, T> {
    return factory { activity: FragmentActivity ->
        ViewModelProviders.of(activity, instance<KodeinAwareViewModelFactory>()).get(T::class.java)
    }
}

inline fun <C, reified T : KodeinAwareViewModel> Kodein.BindBuilder.WithContext<C>.fragmentViewModelFactory():
        Factory<C, Fragment, T> {
    return factory { fragment: Fragment ->
        ViewModelProviders.of(fragment, instance<KodeinAwareViewModelFactory>()).get(T::class.java)
    }
}

inline fun <reified T : KodeinAwareViewModel> Kodein.Builder.bindWithKodeinAwareViewModelFactory() {
    bind<T>() with activityViewModelFactory()
    bind<T>() with fragmentViewModelFactory()
}
