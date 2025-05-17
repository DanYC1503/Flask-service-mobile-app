import pycuda.autoinit
from pycuda import driver as drv
from pycuda.compiler import SourceModule
import numpy as np


def process_image_negative(img_np: np.ndarray, 
                           blocks_x=16, blocks_y=16, threads_x=256, threads_y=1):

    h, w, c = img_np.shape
    size = h * w * c
    flat_img = img_np.flatten().astype(np.uint8)
    output = np.empty_like(flat_img)

    block_dim = (threads_x, threads_y, 1)
    grid_dim = ((size + threads_x - 1) // threads_x, 1, 1)

    ctx = drv.Device(0).make_context()
    try:
        mod_negative = SourceModule("""
        __global__ void negative_filter(unsigned char *img, unsigned char *out, int size) {
            int idx = blockIdx.x * blockDim.x + threadIdx.x;
            if (idx < size) {
                out[idx] = 255 - img[idx];
            }
        }
        """)

        func = mod_negative.get_function("negative_filter")

        d_in = drv.mem_alloc(flat_img.nbytes)
        d_out = drv.mem_alloc(output.nbytes)

        drv.memcpy_htod(d_in, flat_img)

        func(d_in, d_out, np.int32(size), block=block_dim, grid=grid_dim)

        drv.memcpy_dtoh(output, d_out)
    finally:
        ctx.pop()

    output_img = output.reshape((h, w, c))
    return output_img, {'method': 'negative', 'size': size}