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

#sys.stderr.write("Get Word Counting...\n")

f_list   = []
e_list   = []

for ( f, e ) in bitext:
	for f_i in set(f):
		if f_i not in f_list:
			f_list.append(f_i)
	for e_j in set(e):
		if e_j not in e_list:
			e_list.append(e_j)

# Map index 
e_idx    = {}
f_idx    = {}

for (n, f_i) in enumerate(f_list):
	f_idx[ f_i ] = n

for (n, e_j) in enumerate(e_list):
	e_idx[ e_j ] = n

#print f_idx
#print e_idx

# Get number of distinct f, number of distinct e
num_f    = len(f_list)
num_e    = len(e_list)

# Define probability t(e|f)
tef      = np.zeros( ( num_e, num_f ) )
#print num_f, num_e

# Define new probability in E-step
tnef     = np.zeros( ( num_e, num_f ) )
# Initialize probabilities uniformly
tnef.fill( float(1) / float(num_f) )
#print tnef

# tolerance for convergence
tol      = 1e-03
# iteration, and maximum iteration for training
max_iter = 10
it       = 0 

#print "\n"

while True :

	#print "Training epoch: "+ str(it)
	# Consider termination:
	if it >= max_iter:
		#sys.stderr.write("Maximum training iteration has been reached.\n")
		break

	'''Check probabilities convergence is too time-consuming. So skip it'''
	if LA.norm( tnef - tef ) < tol:
		#sys.stderr.write("Probabilities have converged.\n")
		break

	tef      = tnef

	'''E-step'''
	# Initialize Count for C(e|f)
	#print "	E-step:"
	#print "		Initialize Count for C(e|f)"
	count_ef = np.zeros( ( num_e, num_f ) )
	total_f  = np.zeros( num_f )

	#print "		Compute normalization . Collect counts"
	for (n, (f, e)) in enumerate(bitext):
		# Compute normalization
		s_total_e = {}
		for e_j in set(e):
			s_total_e[ e_j ] = 0.0
			for f_i in set(f):
				s_total_e[ e_j ] += tef[ e_idx[e_j] ][ f_idx[f_i] ]

		# Collect counts
		for e_j in set(e):
			for f_i in set(f):
				count_ef[ e_idx[e_j] ][ f_idx[f_i] ] += tef[ e_idx[e_j] ][ f_idx[f_i] ] / s_total_e[ e_j ]
				total_f[ f_idx[f_i] ] += tef[ e_idx[e_j] ][ f_idx[f_i] ] / s_total_e[ e_j ]

	'''M-step'''
	#Estimate probabilities
	#print "	M-step:"
	#print "		Estimate probabilities"
	total_f[ total_f == 0 ] = np.inf
	tnef                    = ( count_ef / total_f )

	it += 1

prob_ef = tnef

# Output Word Alignment
for ( f, e ) in bitext:
	for ( i, f_i ) in enumerate(f):
		max_idx_j = 0
		max_prob  = 0.0
		for ( j, e_j ) in enumerate(e):
			if prob_ef[ e_idx[e_j] ][ f_idx[f_i] ] > max_prob:
				max_idx_j = j
				max_prob  = prob_ef[ e_idx[e_j] ][ f_idx[f_i] ]
		sys.stdout.write( "%i-%i " % ( i, max_idx_j ) )
	sys.stdout.write("\n")	





