package jvn;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import annotations.Read;
import annotations.Write;

public class JvnProxy implements InvocationHandler {
	private JvnObject jo;
	
	private JvnProxy(String name, Object o) throws JvnException {
		JvnServerImpl js = JvnServerImpl.jvnGetServer();
		jo = js.jvnLookupObject(name);
		
		if (jo == null) {
			jo = js.jvnCreateObject((Serializable) o);
			jo.jvnUnLock();
			js.jvnRegisterObject(name, jo);
		}
	}
	
	public static Object newInstance(String name, Object o) throws JvnException {
		return Proxy.newProxyInstance(o.getClass().getClassLoader(), o.getClass().getInterfaces(), new JvnProxy(name, o));
	}

	@Override
	public Object invoke(Object o, Method m, Object[] args) throws Throwable {
		Object result = null;
		
		try {
			if (m.isAnnotationPresent(Read.class)) {
				jo.jvnLockRead();
			} else if (m.isAnnotationPresent(Write.class)) {
				jo.jvnLockWrite();
			}
			
			result = m.invoke(jo.jvnGetObjectState(), args);
			
			jo.jvnUnLock();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
}
