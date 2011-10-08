package me.yonatan.g2g.core.cdi;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import lombok.Cleanup;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.MapMaker;

@Interceptor
@Trace
public class TraceLogInterceptor {

	// Static field is not a very good JavaEE practice, but even if we'll have
	// more than one cache map, it will work.
	private static final Map<Method, String> signatureCache = new MapMaker().softValues().makeComputingMap(new Function<Method, String>() {
		public String apply(Method input) {
			List<String> params = new ArrayList<String>();
			for (Class<?> clazz : input.getParameterTypes()) {
				params.add(clazz.getSimpleName());
			}
			return input.getName() + "(" + (params.size() == 0 ? "" : Joiner.on(',').join(params)) + ")";
		};
	});

	@AroundInvoke
	public Object traceLog(InvocationContext ctx) throws Throwable {
		Logger log = LoggerFactory.getLogger(ctx.getMethod().getDeclaringClass());
		String signatureStr = null;
		if (log.isTraceEnabled()) {
			signatureStr = signatureCache.get(ctx.getMethod());
			log.trace(">>> {}", signatureStr);
		}
		try {
			Object o = ctx.proceed();
			if (log.isTraceEnabled()) {
				log.trace("<<< Return {} by {}", StringUtils.abbreviate((o != null) ? o.toString() : "null", 50), signatureStr);
			}
			return o;
		} catch (Throwable e) {
			if (log.isTraceEnabled()) {
				@Cleanup
				ByteArrayOutputStream baos=new ByteArrayOutputStream();
				@Cleanup
				PrintWriter pw=new PrintWriter(baos);
				e.printStackTrace(pw);
				log.trace("<<< Exception {} thrown by {}\n{} ", new Object[]{(e != null) ? StringUtils.normalizeSpace(e.toString()) : "null", signatureStr,baos.toString()});
			}
			throw e;
		}

	}
}
