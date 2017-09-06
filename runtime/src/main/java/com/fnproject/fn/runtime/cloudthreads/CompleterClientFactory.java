package com.fnproject.fn.runtime.cloudthreads;

import java.io.Serializable;
import java.util.function.Supplier;

public interface CompleterClientFactory extends Supplier<CompleterClient>, Serializable {
}
