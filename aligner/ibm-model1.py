#!/usr/bin/env python
"""
IBM model 1

Author : Tianyi Chen

Date   : Feb 22, 2016

"""
import optparse
import sys
from collections import defaultdict
import numpy as np
from numpy import linalg as LA



"""
Preprocess closet:
=======================
"""
optparser = optparse.OptionParser()
optparser.add_option("-d", "--data", dest="train", default="data/hansards", help="Data filename prefix (default=data)")
optparser.add_option("-e", "--english", dest="english", default="e", help="Suffix of English filename (default=e)")
optparser.add_option("-f", "--french", dest="french", default="f", help="Suffix of French filename (default=f)")
optparser.add_option("-t", "--threshold", dest="threshold", default=0.5, type="float", help="Threshold for aligning with Dice's coefficient (default=0.5)")
optparser.add_option("-n", "--num_sentences", dest="num_sents", default=sys.maxint, type="int", help="Number of sentences to use for training and alignment")
(opts, _) = optparser.parse_args()
f_data = "%s.%s" % (opts.train, opts.french)
e_data = "%s.%s" % (opts.train, opts.english)
"""
=======================
"""

sys.stderr.write("Start Training...\n")
bitext = [[sentence.strip().split() for sentence in pair] for pair in zip(open(f_data), open(e_data))[:opts.num_sents]]

sys.stderr.write("Get Word Counting...\n")
f_count  = defaultdict(int) #define a dict
e_count  = defaultdict(int) #define a dict
fe_count = defaultdict(int)

for (n, (f, e)) in enumerate(bitext):
  for f_i in set(f):
    f_count[f_i] += 1
    for e_j in set(e):
      fe_count[(f_i,e_j)] += 1
  for e_j in set(e):
    e_count[e_j] += 1
  if n % 500 == 0:
    sys.stderr.write(".")

# Map index 
e_idx    = {}
f_idx    = {}

for (n, key) in enumerate(f_count):
	f_idx[key] = n

for (n, key) in enumerate(e_count):
	e_idx[key] = n

#print f_idx
#print e_idx

# Get number of distinct f, number of distinct e
num_f    = len(f_count)
num_e    = len(e_count)

# Define probability t(e|f)
tef      = np.zeros( ( num_e, num_f ) )
#tef.shape = (22231, 28686)

# Define new probability in E-step
tnef     = np.zeros( ( num_e, num_f ) )
# Initialize probabilities uniformly
tnef.fill( float(1) / float(num_f) )

# tolerance for convergence
tol      = 1e-03
# iteration, and maximum iteration for training
max_iter = 1
it       = 0 

print "\n"

while True :

	print "Training epoch: "+ str(it)
	# Consider termination:
	if it >= max_iter:
		sys.stderr.write("Maximum training iteration has been reached.\n")
		break

	'''Check probabilities convergence is too time-consuming. So skip it'''
	# if LA.norm( tnef - tef ) < tol:
	# 	sys.stderr.write("Probabilities have converged.\n")
	# 	break

	tef      = tnef

	'''E-step'''
	# Initialize Count for C(e|f)
	print "	E-step:"
	print "		Initialize Count for C(e|f)"
	count_ef = np.zeros( ( num_e, num_f ) )
	total_f  = np.zeros( num_f )

	print "		Compute normalization . Collect counts"
	for (n, (f, e)) in enumerate(bitext):
		# Compute normalization
		for e_word in set(e):
			s_total_e = 0.0
			for f_word in set(f):
				s_total_e += tef[ e_idx[e_word] ][ f_idx[f_word] ]
		# Collect counts
		for e_word in set(e):
			for f_word in set(f):
				count_ef[ e_idx[e_word] ][ f_idx[f_word] ] += tef[ e_idx[e_word] ][ f_idx[f_word] ] / s_total_e
				total_f[ f_idx[f_word] ] += tef[ e_idx[e_word] ][ f_idx[f_word] ] / s_total_e
				

	'''M-step'''
	#Estimate probabilities
	print "	M-step:"
	print "		Estimate probabilities"
	total_f[ total_f == 0 ] = np.inf
	tnef                    = ( count_ef / total_f )

	it += 1

pef = tnef

# Output Word Alignment
for ( f, e ) in bitext:
	for ( i, f_i ) in enumerate(f):
		max_j = np.argmax( pef[ : , f_idx[f_i] ] )
		sys.stdout.write("%i-%i " % (i,max_j))
	sys.stdout.write("\n")

	





