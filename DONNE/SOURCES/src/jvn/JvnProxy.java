package jvn;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import annotations.Read;
import annotations.Write;

public class JvnProxy implements InvocationHandler {
	private JvnObject jo;
	
	private JvnProxy(String name, Class c) throws JvnException {
		JvnServerImpl js = JvnServerImpl.jvnGetServer();
		jo = js.jvnLookupObject(name);
		
		if (jo == null) {
			try {
				jo = js.jvnCreateObject((Serializable) c.newInstance());
				jo.jvnUnLock();
				js.jvnRegisterObject(name, jo);
			} catch (InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
				throw new JvnException("Wrong class");
			}
		}
	}
	
	public static Object newInstance(String name, Class c) throws JvnException {
		return Proxy.newProxyInstance(c.getClassLoader(), c.getInterfaces(), new JvnProxy(name, c));
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
