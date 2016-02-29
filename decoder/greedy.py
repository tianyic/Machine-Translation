#!/usr/bin/env python
import sys
import optparse
import models
import copy

class TranslationPair:
	"""
	POJO for translation information
	"""
	def __init__(self, logprob, lm_state, predecessor, phrase,
	 fPhrase, srcStartPos, srcEndPos, srcWordCounts, srcPhraseCounts, tgtStartPos, tgtEndPos):
		self.logprob = logprob
		self.lm_state = lm_state
		self.predecessor = predecessor
		self.phrase = phrase
		self.fPhrase = fPhrase
		self.srcStartPos = srcStartPos
		self.srcEndPos = srcEndPos
		self.srcWordCounts = srcWordCounts
		self.srcPhraseCounts = srcPhraseCounts
		self.tgtStartPos = tgtStartPos
		self.tgtEndPos = tgtEndPos

class GreedyDecoder:
	"""Greedy Decoder"""
	def __init__(self, opts):
		self.tm = models.TM(opts.tm, opts.k)
		self.lm = models.LM(opts.lm)
		self.french = [tuple(line.strip().split()) for line in open(opts.input).readlines()[:opts.num_sents]]
		self.alpha = opts.alpha
		self.distantThreshold = opts.dt

		# tm should translate unknown words as-is with probability 1
		for word in set(sum(self.french,())):
			if (word,) not in self.tm:
				self.tm[(word,)] = [models.phrase(word, 0.0)]

		sys.stderr.write("Decoding %s...\n" % (opts.input,))

	def train(self):
		for f in self.french:
			h = self.hillClimb(f)
			for pair in h:
				if pair.predecessor is not None:
					sys.stdout.write(pair.phrase.english + ' ')
			sys.stdout.write('\n')

	def seed(self, f):
		"""
		Choose an initial segmentation
		Currently use stackDecode as seed, should consider coverage segmentation
		"""
		return self.stackDecode(f)

	def stackDecode(self, f):
		"""
		Below comes from sample stack decoding program, but the tuple has been modified, 
		to record more location information
		Note: a distortion probability is considered
		"""
		translationPairList = []
		initial_translationPair = TranslationPair(0.0, self.lm.begin(), None, None,None, 0, 0, 0, 0, 0, 0)
		stacks = [{} for _ in f] + [{}]
		stacks[0][self.lm.begin()] = initial_translationPair
		for i, stack in enumerate(stacks[:-1]):
			for h in sorted(stack.itervalues(),key=lambda h: -h.logprob)[:1]: # prune
				for j in xrange(i+1,len(f)+1):
					if f[i:j] in self.tm:
						for phrase in self.tm[f[i:j]]:
							logprob = h.logprob + phrase.logprob
							phrase_count = h.srcPhraseCounts + 1
							lm_state = h.lm_state
							tgtEndPos = h.tgtEndPos
							for word in phrase.english.split():
								tgtEndPos += 1
								(lm_state, word_logprob) = self.lm.score(lm_state, word)
								logprob += word_logprob

							logprob -= self.distortionProb(h.tgtEndPos, tgtEndPos)
							logprob += self.lm.end(lm_state) if j == len(f) else 0.0
							new_translationPair = TranslationPair(logprob, \
								lm_state, h, phrase,f[i:j], i, j, i, phrase_count,h.tgtEndPos, tgtEndPos)
							if lm_state not in stacks[j] or stacks[j][lm_state].logprob < logprob: # second case is recombination
								stacks[j][lm_state] = new_translationPair

		winner = max(stacks[-1].itervalues(), key=lambda h: h.logprob)
		def extract_english(h): 
			if h.predecessor is not None:
				extract_english(h.predecessor)
			translationPairList.append(h)
			return

		extract_english(winner)

		return translationPairList

	def score(self, current):
		"""
		Compute the score for current translation
		current - a list of current translation for one sentence
		"""
		logprob = 0.0
		lm_state = self.lm.begin()
		for pair in current:
			if pair.phrase != None and pair.fPhrase != None:
				logprob += pair.phrase.logprob
				for word in pair.phrase.english.split():
					(lm_state, word_logprob) = self.lm.score(lm_state, word)
					logprob += word_logprob

				logprob -= self.distortionProb(pair.tgtStartPos, pair.tgtEndPos)
		logprob += self.lm.end(lm_state)
		# print 'score : ' + str(logprob)
		return logprob

	def distortionProb(self, i,j):
		"""
		i is the start position of foreigh language
		j is the end position of English phrase
		"""
		return self.alpha**abs(i-j-1)

	def hillClimb(self, source):
		"""
		Hill climb to find best translation
		"""
		current = self.seed(source)
		while True:
			s_current = self.score(current)
			s = s_current
			best = current
			for h in self.neighborhood(current):
				c = self.score(h)
				if c > s:
					s = c
					best = h
			if s == s_current:
				return current
			else:
				current = best

	def neighborhood(self, current):
		"""
		Do all transformation to explore better translation option
		"""
		return self.move(current) + self.swap(current) + self.replace(current) +\
		 self.split(current) + self.merge(current) + self.bireplace(current)
		
	def move(self, current):
		"""
		whenever two adjacent source phrases are translated by phrases that are distant,
		we consider moving one of the translation closer to the other
		"""
		moveList = []
		sortCurrent = sorted(current, key=lambda m: m.srcStartPos)
		for i in xrange(1,len(sortCurrent)-1):
			pair = current[i]
			adj = current[i+1]
			if abs(adj.tgtStartPos - pair.tgtStartPos) >= self.distantThreshold:
					mov = copy.deepcopy(sortCurrent)
					moveList.append(self.moveForward(mov,i))
					moveList.append(self.moveBackward(mov,i))
				
		return moveList

	def moveForward(self, mov, i):
		pair = mov[i]
		adj = mov[i+1]
		if adj.tgtStartPos > pair.tgtStartPos:
			moveLen = pair.tgtEndPos - pair.tgtStartPos
			for mp in mov:
				if mp.tgtStartPos > pair.tgtStartPos and mp.tgtStartPos < adj.tgtStartPos:
					mp.tgtStartPos -= moveLen
					mp.tgtEndPos -= moveLen
			pair.tgtEndPos = adj.tgtStartPos
			pair.tgtStartPos = pair.tgtEndPos - moveLen
		else:
			moveLen = adj.tgtEndPos - adj.tgtStartPos
			for mp in mov:
				if mp.tgtStartPos > adj.tgtStartPos and mp.tgtStartPos < pair.tgtStartPos:
					mp.tgtStartPos -= moveLen
					mp.tgtEndPos -= moveLen
			adj.tgtEndPos = adj.tgtStartPos
			adj.tgtStartPos = adj.tgtEndPos - moveLen
		
		return sorted(mov, key=lambda m: m.tgtStartPos)

	def moveBackward(self, mov, i):
		pair = mov[i]
		adj = mov[i+1]
		if adj.tgtStartPos > pair.tgtStartPos:
			moveLen = adj.tgtEndPos - adj.tgtStartPos
			for mp in mov:
				if mp.tgtStartPos > pair.tgtStartPos and mp.tgtStartPos < adj.tgtStartPos:
					mp.tgtStartPos += moveLen
					mp.tgtEndPos += moveLen
			adj.tgtStartPos = pair.tgtEndPos
			adj.tgtEndPos = adj.tgtStartPos + moveLen
		else:
			moveLen = pair.tgtEndPos - pair.tgtStartPos
			for mp in mov:
				if mp.tgtStartPos > adj.tgtStartPos and mp.tgtStartPos < pair.tgtStartPos:
					mp.tgtStartPos += moveLen
					mp.tgtEndPos += moveLen
			pair.tgtStartPos = adj.tgtEndPos
			pair.tgtEndPos = pair.tgtStartPos + moveLen
		
		return sorted(mov, key=lambda m: m.tgtStartPos)

	def swap(self, current):
		"""
		Swap every 2 source phrase pair
		"""
		swapList = []
		for i in xrange(1,len(current)-1):
			for j in xrange(i+1, len(current)-1):
				swp = copy.deepcopy(current)
				pair = swp[i]
				adj = swp[j]
				pairLen = pair.tgtEndPos - pair.tgtStartPos
				adjLen = adj.tgtEndPos - adj.tgtStartPos
				# Swap target position, and rearrange all elements between them
				if pair.tgtStartPos > adj.tgtStartPos:
					for m in swp:
						if m.tgtStartPos > adj.tgtStartPos and m.tgtStartPos < pair.tgtStartPos:
							m.tgtStartPos = m.tgtStartPos - adjLen + pairLen
							m.tgtEndPos = m.tgtEndPos - adjLen + pairLen
					pair.tgtStartPos = adj.tgtStartPos
					adj.tgtEndPos = pair.tgtEndPos
					pair.tgtEndPos = pair.tgtStartPos + pairLen
					adj.tgtStartPos = adj.tgtEndPos - adjLen
				else:
					for m in swp:
						if m.tgtStartPos > pair.tgtStartPos and m.tgtStartPos < adj.tgtStartPos:
							m.tgtStartPos = m.tgtStartPos - pairLen + adjLen
							m.tgtEndPos = m.tgtEndPos - pairLen + adjLen
					adj.tgtStartPos = pair.tgtStartPos
					pair.tgtEndPos = adj.tgtEndPos
					adj.tgtEndPos = adj.tgtStartPos + adjLen
					pair.tgtStartPos = pair.tgtEndPos - pairLen
				# Sort
				swp = sorted(swp, key=lambda m: m.tgtStartPos)
				swapList.append(swp)

		return swapList

	def replace(self, current):
		"""
		Replace one translation with other possibilities
		"""
		replaceList = []
		for i in xrange(1,len(current)-1):
			pair = current[i]
			diffLen = 0
			if pair.fPhrase in self.tm:
				for phrase in self.tm[pair.fPhrase]:
					rep = copy.deepcopy(current)
					newPair = rep[i]
					# only replace if it is a different phrase
					if phrase != newPair.phrase:
						diffLen = len(phrase.english.split()) - len(newPair.phrase.english.split())
						newPair.phrase = phrase
						newPair.tgtEndPos = (newPair.tgtEndPos - newPair.tgtStartPos) + diffLen
						for m in rep:
							if m.tgtStartPos > newPair.tgtStartPos:
								m.tgtStartPos += diffLen
								m.tgtEndPos += diffLen
						# No need to sort, already sorted
						replaceList.append(rep)

		if not replaceList:
			replaceList.append(current)

		return replaceList

	def bireplace(self, current):
		"""
		Replace adjacent source language translation 
		"""
		bireplaceList = []
		for i in xrange(1,len(current)-1):
			pair = current[i]
			adj = current[i+1]
			if pair.fPhrase in self.tm and adj.fPhrase in self.tm:
				for phrase1 in self.tm[pair.fPhrase]:
					for phrase2 in self.tm[adj.fPhrase]:
						if phrase1 != pair.phrase and phrase2 != adj.phrase:
							birep = copy.deepcopy(current)
							rep1 = birep[i]
							rep2 = birep[i+1]
							diffLen1 = len(phrase1.english.split()) - len(rep1.phrase.english.split())
							diffLen = diffLen1 + len(phrase2.english.split()) - len(rep2.phrase.english.split())
							rep1.phrase = phrase1
							rep2.phrase = phrase2
							rep1.tgtEndPos = (rep1.tgtEndPos - rep1.tgtStartPos) + diffLen1
							for m in birep:
								if m.tgtStartPos > rep2.tgtStartPos:
									m.tgtStartPos += diffLen
									m.tgtEndPos += diffLen
							rep2.tgtEndPos = (rep2.tgtEndPos - rep2.tgtStartPos) + diffLen
							rep2.tgtStartPos = rep1.tgtEndPos
							
							bireplaceList.append(birep)

		if not bireplaceList:
			bireplaceList.append(current)

		return bireplaceList

	def split(self, current):
		"""
		Split a translation into 2 words
		"""
		splitList = []
		for i in range(len(current)-1):
			if current[i].fPhrase is None:
				continue
			pair = current[i]
			fLen = len(pair.fPhrase)
			eLen = len(pair.phrase.english.split())
			for j in xrange(1,fLen):
				f1 = pair.fPhrase[0:j]
				f2 = pair.fPhrase[j:]
				# If both in our table, try all possible combinations
				if f1 in self.tm and f2 in self.tm:
					for phrase1 in self.tm[f1]:
						for phrase2 in self.tm[f2]:
							spl = copy.deepcopy(current)
							uPair = spl[i]
							p1Len = len(phrase1.english.split())
							p2Len = len(phrase2.english.split())
							# Compute length difference, if changed, update all pair after this
							lenDiff = eLen - (p1Len + p2Len)
							if lenDiff != 0:
								for m in spl:
									if m.tgtStartPos > uPair.tgtStartPos:
										m.tgtStartPos -= lenDiff
										m.tgtEndPos -= lenDiff
							# update split words
							p2Len = len(phrase2.english.split())
							newPair = copy.deepcopy(uPair)
							# Update pair
							uPair.fPhrase = f1
							uPair.phrase = phrase1
							uPair.tgtEndPos = uPair.tgtStartPos + p1Len
							# Update new pair
							newPair.fPhrase = f2
							newPair.phrase = phrase2
							newPair.tgtStartPos = uPair.tgtEndPos
							newPair.tgtEndPos = newPair.tgtStartPos + p2Len
							newPair.srcEndPos = uPair.srcEndPos
							newPair.srcStartPos = newPair.srcEndPos - len(f2)
							uPair.srcEndPos = uPair.srcStartPos + len(f1)
							spl.insert(i+1, newPair)
							# Add to split list
							spl = sorted(spl, key=lambda m: m.tgtStartPos)
							splitList.append(spl)
							
		# Prevent from null
		if not splitList:
			splitList.append(current)

		return splitList

	def merge(self, current):
		"""
		Merge adjacent source phrase
		"""
		# Sort as source start position
		mergeList = []
		sortCurrent = sorted(current, key=lambda m: m.srcStartPos)

		for i in xrange(1,len(sortCurrent)-1):
			pair = sortCurrent[i]
			adj = sortCurrent[i+1]
			newF = pair.fPhrase + adj.fPhrase
			e1Len = pair.tgtEndPos - pair.tgtStartPos
			e2Len = adj.tgtEndPos - adj.tgtStartPos
			if newF in self.tm:
				for phrase in self.tm[newF]:
					merg = copy.deepcopy(sortCurrent)
					m = merg[i]
					merg[i].fPhrase = newF
					merg[i].phrase = phrase
					merg[i].srcEndPos = adj.srcEndPos
					# Remove
					merg.remove(merg[i+1])
					# Move following elements
					lenDiff = len(phrase.english.split()) - e1Len - e2Len
					if lenDiff != 0:
						for p in merg:
							if p.tgtStartPos > merg[i].tgtStartPos:
								p.tgtStartPos += lenDiff
								p.tgtEndPos += lenDiff
					merg[i].tgtEndPos = adj.tgtEndPos + lenDiff
					merg = sorted(merg, key=lambda m: m.tgtStartPos)
					mergeList.append(merg)
			# Try a 3 phrase merge
			if (i + 2) <= len(sortCurrent)-1:
				adj2 = sortCurrent[i+2]
				newF = pair.fPhrase + adj.fPhrase + adj2.fPhrase
				e3Len = adj2.tgtEndPos - adj2.tgtStartPos
				if newF in self.tm:
					for phrase in self.tm[newF]:
						merg = copy.deepcopy(sortCurrent)
						m = merg[i]
						merg[i].fPhrase = newF
						merg[i].phrase = phrase
						merg[i].srcEndPos = adj2.srcEndPos
						# Remove merged elements
						m2 = merg[i+2]
						merg.remove(merg[i+1])
						merg.remove(m2)
						# Move following elements
						lenDiff = len(phrase.english.split()) - e1Len - e2Len - e3Len
						if lenDiff != 0:
							for p in merg:
								if p.tgtStartPos > merg[i].tgtStartPos:
									p.tgtStartPos += lenDiff
									p.tgtEndPos += lenDiff
						merg[i].tgtEndPos = adj2.tgtEndPos + lenDiff
						merg = sorted(merg, key=lambda m: m.tgtStartPos)	
						#For index
						mergeList.append(merg)

		if not mergeList:
			mergeList.append(current)
		return mergeList


def main(argv):
	optparser = optparse.OptionParser()
	optparser.add_option("-i", "--input", dest="input", default="data/input", help="File containing sentences to translate (default=data/input)")
	optparser.add_option("-t", "--translation-model", dest="tm", default="data/tm", help="File containing translation model (default=data/tm)")
	optparser.add_option("-l", "--language-model", dest="lm", default="data/lm", help="File containing ARPA-format language model (default=data/lm)")
	optparser.add_option("-n", "--num_sentences", dest="num_sents", default=sys.maxint, type="int", help="Number of sentences to decode (default=no limit)")
	optparser.add_option("-k", "--translations-per-phrase", dest="k", default=5, type="int", help="Limit on number of translations to consider per phrase (default=1)")
	optparser.add_option("-a", "--alpha-base", dest="alpha", default=0.05, type="float", help="Distortion Constant (default=0.005)")
	optparser.add_option("-s", "--stack-size", dest="s", default=1, type="int", help="Maximum stack size (default=1)")
	optparser.add_option("-d", "--distortion", dest="dt", default=1, type="int", help="Distortion threshold for move(default=1)")
	opts = optparser.parse_args()[0]

	gd = GreedyDecoder(opts)
	gd.train()

if __name__=='__main__':
	main(sys.argv)