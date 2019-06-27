#include <stdio.h>
#include "utility.h"

void calc(int operand1, int operand2) {
#if defined(CONFIG_ADDITION)

#ifdef CONFIG_DEBUG
    printf("Addition");
#endif

    int result = add(operand1, operand2);
    char op = '+';

#elif defined(CONFIG_SUBTRACTION) /*End CONFIG_ADDITION*/

#ifdef CONFIG_DEBUG
    printf("Substraction");
#endif

    int result = sub(operand1, operand2);
    char op = '-';
#endif /*End CONFIG_SUBSTRACTION*/
    
    printf("%i %c %i = %i\n", operand1, op, operand2, result);
}

int main(int argc, char **argv) {
#ifdef CONFIG_DEBUG
    printf(MODE);
#endif

    printf("Calculation Example\n");

    if (CONFIG_ADDITION) {
        printf("Adding 37 to 73\n");
    } else if (CONFIG_SUBSTRACTION) {
        printf("Substracting 37 from 73\n");
    } else {
        printf("No operation specified; nothing to calculate\n");
    }
	
#ifdef CONFIG_CALCULATION
    calc(73, 37);
#endif
    
    return 0;
}
