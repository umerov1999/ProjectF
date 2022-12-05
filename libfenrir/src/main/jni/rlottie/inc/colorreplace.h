#ifndef _ColorReplace_H_
#define _ColorReplace_H_
#include <map>

#define RLOTTIE_API

namespace rlottie {
	namespace internal {
		class RLOTTIE_API ColorReplace {
		public:
			ColorReplace(bool useMoveColor = false) {
				this->useMoveColor = useMoveColor;
			}
			ColorReplace(const std::map<int32_t, int32_t>&colorMap, bool useMoveColor = false) {
				this->colorMap = colorMap;
				this->useMoveColor = useMoveColor;
			}
			std::map<int32_t, int32_t>colorMap;
			bool useMoveColor;
		};
	}
}
#endif
