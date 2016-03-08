# Greedy Decoder 
### Author: Tianyi Chen, tchen59@jhu.edu
### Date: March 7, 2016
### Language: Java

-`greedy-decoder` translates input sentences from French to English by the method described in  

"A Greedy Decoder for Phrase-Based Statistical Machine Translation"

There is one parameter that can be controlled k---the number of English phrases for one French phrase.

To run the code, use the following commands in Terminal:

	> ./greedy-decoder k
e.g.

	> ./greedy-decoder 10
Then English tranlasted sentences will be generated into output directory, the output file name has the form:

	> output_k_x.txt
e.g.

	> output_k_10.txt


-`grade` computes the model score of a translated sentence from Philipp Koehn. To calculate the score of translation, run the following command in Terminal:

    > python compute-model-score < output/output_k_x.txt
e.g.

	> python compute-model-score < output/output_k_10.txt

Thank you very much!

Best Regards,

Tianyi
